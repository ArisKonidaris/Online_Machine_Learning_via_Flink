/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except request compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to request writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package oml

import oml.mlAPI.math.Point
import oml.FlinkBipartiteAPI.operators.{FlinkHub, FlinkSpoke}
import oml.FlinkBipartiteAPI.messages.{ControlMessage, HubMessage, SpokeMessage}
import oml.FlinkBipartiteAPI.POJOs.{DataInstance, QueryResponse, Request}
import oml.FlinkBipartiteAPI.utils._
import oml.FlinkBipartiteAPI.utils.KafkaUtils._
import oml.FlinkBipartiteAPI.utils.deserializers.{DataInstanceDeserializer, RequestDeserializer}
import oml.FlinkBipartiteAPI.utils.parsers.dataStream.DataPointParser
import oml.FlinkBipartiteAPI.utils.parsers.requestStream.PipelineMap
import oml.FlinkBipartiteAPI.utils.partitioners.random_partitioner
import oml.FlinkBipartiteAPI.utils.serializers.GenericSerializer
import oml.StarTopologyAPI.sites.{NodeId, NodeType}
import oml.mlAPI.mlworkers.generators.MLNodeGenerator
import org.apache.flink.api.common.functions.RichFlatMapFunction
import org.apache.flink.api.common.serialization.TypeInformationSerializationSchema
import org.apache.flink.api.java.utils.ParameterTool
import org.apache.flink.streaming.api.TimeCharacteristic
import org.apache.flink.streaming.api.scala._
import org.apache.flink.streaming.connectors.kafka.{FlinkKafkaConsumer, FlinkKafkaProducer}
import org.apache.flink.util.Collector

/**
  * Interactive Online Machine Learning Flink Streaming Job.
  */
object OML_Job {

  val queryResponse: OutputTag[QueryResponse] = OutputTag[QueryResponse]("QueryResponse")

  def main(args: Array[String]) {

    /** Set up the streaming execution environment */
    implicit val env: StreamExecutionEnvironment = StreamExecutionEnvironment.getExecutionEnvironment
    implicit val params: ParameterTool = ParameterTool.fromArgs(args)

    env.getConfig.setGlobalJobParameters(params)
    env.setParallelism(params.get("parallelism", DefaultJobParameters.defaultParallelism).toInt)
    oml.FlinkBipartiteAPI.utils.CommonUtils.registerFlinkMLTypes(env)
    env.setStreamTimeCharacteristic(TimeCharacteristic.ProcessingTime)
    if (params.get("checkpointing", "false").toBoolean) Checkpointing.enableCheckpointing()


    ////////////////////////////////////////////// Kafka Connectors ////////////////////////////////////////////////////


    /** The coordinator messages. */
    val psMessages: DataStream[HubMessage] = env.addSource(
      new FlinkKafkaConsumer[HubMessage]("psMessages",
        new TypeInformationSerializationSchema(createTypeInformation[HubMessage], env.getConfig),
        createProperties("psMessagesAddr", "psMessages_Consumer"))
        .setStartFromLatest())
//      .addSource(KafkaUtils.KafkaTypeConsumer[HubMessage]("psMessages"))

    /** The incoming training data. */
    val trainingSource: DataStream[DataInstance] = env.addSource(
      new FlinkKafkaConsumer[DataInstance]("trainingData",
        new DataInstanceDeserializer(true),
        createProperties("trainingDataAddr", "trainingDataConsumer"))
        .setStartFromEarliest())
      .name("TrainingSource")

    /** The incoming requests. */
    val requests: DataStream[Request] = env.addSource(
      new FlinkKafkaConsumer[Request]("requests",
        new RequestDeserializer(true),
        createProperties("requestsAddr", "requestsConsumer"))
        .setStartFromEarliest())
      .name("RequestSource")


    /////////////////////////////////////////// Data and Request Parsing ///////////////////////////////////////////////


    /** Parsing the training data */
    val trainingData: DataStream[Point] = trainingSource
      .flatMap(new DataPointParser)
      .name("DataParsing")

    /** Check the validity of the request */
    val validRequest: DataStream[ControlMessage] = requests
      .keyBy((_: Request) => 0)
      .flatMap(new PipelineMap)
      .setParallelism(1)
      .name("RequestParsing")


    /////////////////////////////////////////////////// Training ///////////////////////////////////////////////////////

    /** The broadcast messages of the Hub. */
    val coordinatorMessages: DataStream[ControlMessage] = psMessages
      .flatMap(new RichFlatMapFunction[HubMessage, ControlMessage] {
        override def flatMap(in: HubMessage, out: Collector[ControlMessage]): Unit = {
          for ((rpc, dest) <- in.operations zip in.destinations)
            out.collect(ControlMessage(in.getNetworkId, rpc, in.getSource, dest, in.getData, in.getRequest))
        }
      })

    /** Partitioning the Hub's messages along with the request messages to the workers. */
    val controlMessages: DataStream[ControlMessage] = coordinatorMessages
      .partitionCustom(random_partitioner, (x: ControlMessage) => x.destination.getNodeId)
      .union(validRequest.partitionCustom(random_partitioner, (x: ControlMessage) => x.destination.getNodeId))

    /** Partitioning the training data along with the control messages to the workers. */
    val trainingDataBlocks: ConnectedStreams[Point, ControlMessage] = trainingData
      .connect(controlMessages)

    /** The parallel learning procedure happens here. */
    val worker: DataStream[SpokeMessage] = trainingDataBlocks
      .process(new FlinkSpoke[MLNodeGenerator])
      .name("FlinkSpoke")

    /** The coordinator operators, where the learners are merged. */
    val coordinator: DataStream[HubMessage] = worker
      .keyBy((x: SpokeMessage) => x.getNetworkId + "_" + x.getDestination.getNodeId)
      .process(new FlinkHub[MLNodeGenerator])
      .name("FlinkHub")

    /** The Kafka iteration for emulating parameter server messages */
    coordinator
      .addSink(KafkaUtils.kafkaTypeProducer[HubMessage]("psMessages"))
      .name("FeedbackLoop")


    //////////////////////////////////////////////// Sinks /////////////////////////////////////////////////////////////


    /** A Kafka Sink for the query responses. */
    worker
      .getSideOutput(queryResponse)
      .addSink(
        new FlinkKafkaProducer[QueryResponse](params.get("responsesAddr", "localhost:9092"),
          "responses",
          new GenericSerializer[QueryResponse])
      ).setParallelism(1)
      .name("ResponsesSink")


    //////////////////////////////////////////// Execute OML Job ///////////////////////////////////////////////////////


    /** execute program */
    env.execute(params.get("jobName", DefaultJobParameters.defaultJobName))
  }

}
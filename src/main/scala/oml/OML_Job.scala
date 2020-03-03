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

import oml.logic.{ParameterServer, Worker}
import oml.message.mtypes.{ControlMessage, DataPoint, workerMessage}
import oml.mlAPI.mlworkers.MLWorkerGenerator
import oml.utils.KafkaUtils
import oml.utils.KafkaUtils.createProperties
import oml.POJOs.Request
import oml.utils.deserializers.RequestDeserializer
import oml.utils.parsers.dataStream.CsvDataParser
import oml.utils.parsers.requestStream.PipelineMap
import oml.utils.partitioners.random_partitioner
import org.apache.flink.api.java.utils.ParameterTool
import org.apache.flink.streaming.api.scala._
import org.apache.flink.streaming.connectors.kafka.FlinkKafkaConsumer

/**
  * Interactive Online Machine Learning Flink Streaming Job.
  */
object OML_Job {

  def main(args: Array[String]) {

    /** Set up the streaming execution environment */
    implicit val env: StreamExecutionEnvironment = StreamExecutionEnvironment.getExecutionEnvironment
    implicit val params: ParameterTool = ParameterTool.fromArgs(args)

    env.getConfig.setGlobalJobParameters(params)
    env.setParallelism(params.get("parallelism", utils.DefaultJobParameters.defaultParallelism).toInt)
    oml.common.OMLTools.registerFlinkMLTypes(env)
    if (params.get("checkpointing", "false").toBoolean) utils.Checkpointing.enableCheckpointing()


    /** The parameter server messages */
    val psMessages: DataStream[ControlMessage] = env
      .addSource(KafkaUtils.KafkaTypeConsumer[ControlMessage]("psMessages"))

    /** The incoming data */
    val data: DataStream[String] = env
      .addSource(KafkaUtils.KafkaStringConsumer("data"))

    /** The incoming requests */
    val requests: DataStream[Request] = env.addSource(
      new FlinkKafkaConsumer[Request]("requests",
        new RequestDeserializer(true),
        createProperties("requestsAddr", "requests_Consumer"))
        .setStartFromEarliest()
    )


    /** Parsing the data */
    val parsed_data: DataStream[DataPoint] = data
      .flatMap(new CsvDataParser)

    /** Check the validity of the request */
    val valid_request: DataStream[ControlMessage] = requests
      .keyBy((_: Request) => 0)
      .flatMap(new PipelineMap)
      .setParallelism(1)


    /** partitioning the Parameter Server's messages along with the requests to the workers */
    val controlMessages: DataStream[ControlMessage] = psMessages
      .partitionCustom(random_partitioner, (x: ControlMessage) => x.workerID)
      .union(
        valid_request
          .filter(x => x.container.get.request != "Query")
          .partitionCustom(random_partitioner, (x: ControlMessage) => x.workerID)
      )

    /** Partitioning the data to the workers */
    val data_blocks: ConnectedStreams[DataPoint, ControlMessage] = parsed_data
      .connect(controlMessages)


    /** The parallel learning procedure happens here */
    val worker: DataStream[workerMessage] = data_blocks.flatMap(new Worker[MLWorkerGenerator])

    /** The coordinator logic, where the learners are merged */
    val coordinator: DataStream[ControlMessage] = worker
      .keyBy((x: workerMessage) => x.nodeID)
      .flatMap(new ParameterServer)


    /** The Kafka iteration for emulating parameter server messages */
    coordinator
      .addSink(KafkaUtils.kafkaTypeProducer[ControlMessage]("psMessages"))

    /** For debugging */
    coordinator
      .map(x => System.nanoTime + " , " + x.toString)
      .addSink(KafkaUtils.kafkaStringProducer("psMessagesStr"))


    /** execute program */
    env.execute(params.get("jobName", utils.DefaultJobParameters.defaultJobName))
  }

}
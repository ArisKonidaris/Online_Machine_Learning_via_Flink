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

import java.util.Properties

import oml.logic.{ParameterServer, Predictor, Trainer}
import oml.message.mtypes.{ControlMessage, workerMessage}
import oml.mlAPI.mlworkers.MLNodeGenerator
import oml.utils.{CommonUtils, KafkaUtils}
import oml.POJOs.{DataInstance, Prediction, QueryResponse, Request}
import oml.math.Point
import oml.parameters.ParameterDescriptor
import oml.utils.parsers.{DataInstanceParser, RequestParser}
import oml.utils.parsers.dataStream.DataPointParser
import oml.utils.parsers.requestStream.PipelineMap
import oml.utils.partitioners.random_partitioner
import org.apache.flink.api.common.functions.RichFlatMapFunction
import org.apache.flink.api.java.utils.ParameterTool
import org.apache.flink.streaming.api.TimeCharacteristic
import org.apache.flink.streaming.api.scala.{ConnectedStreams, _}
import org.apache.flink.streaming.connectors.kafka.{FlinkKafkaConsumer, FlinkKafkaProducer}
import org.apache.flink.api.common.serialization.{SimpleStringSchema, TypeInformationSerializationSchema}
import org.apache.flink.util.Collector

/**
  * Interactive Online Machine Learning Flink Streaming Job.
  */
object OML_Job {

  def createProperties(brokerList: String, group_id: String)(implicit params: ParameterTool): Properties = {
    val properties: Properties = new Properties()
    properties.setProperty("bootstrap.servers", params.get(brokerList, "localhost:9092"))
    properties.setProperty("group.flink_worker_id", group_id)
    properties
  }

  val queryResponse: OutputTag[QueryResponse] = OutputTag[QueryResponse]("QueryResponse")

  def main(args: Array[String]) {

    /** Set up the streaming execution environment */
    implicit val env: StreamExecutionEnvironment = StreamExecutionEnvironment.getExecutionEnvironment
    implicit val params: ParameterTool = ParameterTool.fromArgs(args)

    env.getConfig.setGlobalJobParameters(params)
    env.setParallelism(params.get("parallelism", utils.DefaultJobParameters.defaultParallelism).toInt)
    CommonUtils.registerFlinkMLTypes(env)
    env.setStreamTimeCharacteristic(TimeCharacteristic.ProcessingTime)
    if (params.get("checkpointing", "false").toBoolean) utils.Checkpointing.enableCheckpointing()


    ////////////////////////////////////////////// Kafka Connectors ////////////////////////////////////////////////////


    /** The parameter server messages. */
    val psMessages: DataStream[ControlMessage] = env
      .addSource(new FlinkKafkaConsumer[ControlMessage](
        params.get("psMessagesTopic", "psMessages"),
        new TypeInformationSerializationSchema(createTypeInformation[ControlMessage], env.getConfig),
        createProperties("psMessagesAddr", "psMessagesConsumer"))
        .setStartFromLatest())
      .name("FeedBackLoopSource")

    /** The incoming training data. */
    val trainingSource: DataStream[DataInstance] = env.addSource(
      new FlinkKafkaConsumer[String](params.get("trainingDataTopic", "trainingData"),
        new SimpleStringSchema(),
        createProperties("trainingDataAddr", "trainingDataConsumer"))
        .setStartFromEarliest())
      .flatMap(DataInstanceParser())
      .name("TrainingSource")

    /** The incoming forecasting data. */
    val forecastingSource: DataStream[DataInstance] = env.addSource(
      new FlinkKafkaConsumer[String](params.get("forecastingDataTopic", "forecastingData"),
        new SimpleStringSchema(),
        createProperties("forecastingDataAddr", "forecastingDataConsumer"))
        .setStartFromEarliest())
      .flatMap(DataInstanceParser())
      .name("ForecastingSource")

    /** The incoming requests */
    val requests: DataStream[Request] = env.addSource(
      new FlinkKafkaConsumer[String](params.get("requestsTopic", "requests"),
        new SimpleStringSchema(),
        createProperties("requestsAddr", "requestsConsumer"))
        .setStartFromEarliest())
      .flatMap(RequestParser())
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


    /** partitioning the Parameter Server's messages along with the requests to the workers. */
    val controlMessages: DataStream[ControlMessage] = psMessages
      .partitionCustom(random_partitioner, (x: ControlMessage) => x.workerID)
      .union(
        validRequest.partitionCustom(random_partitioner, (x: ControlMessage) => x.workerID)
      )

    /** Partitioning the training data along with the control messages to the workers. */
    val trainingDataBlocks: ConnectedStreams[Point, ControlMessage] = trainingData
      .connect(controlMessages)


    /** The parallel learning procedure happens here. */
    val worker: DataStream[workerMessage] = trainingDataBlocks
      .process(new Trainer[MLNodeGenerator])
      .name("Trainer")


    /** The coordinator logic, where the learners are merged. */
    val coordinator: DataStream[ControlMessage] = worker
      .keyBy((x: workerMessage) => x.nodeID)
      .flatMap(new ParameterServer)
      .name("ParameterServer")


    /** The Kafka iteration for emulating parameter server messages. */
    coordinator
      .addSink(new FlinkKafkaProducer[ControlMessage](
        params.get("psMessagesAddr", "localhost:9092"), // broker list
        params.get("psMessagesTopic", "psMessages"), // target topic
        new TypeInformationSerializationSchema(createTypeInformation[ControlMessage], env.getConfig)))
      .name("FeedbackLoop")


    ////////////////////////////////////////////////// Predicting //////////////////////////////////////////////////////


    val modelUpdates: DataStream[ControlMessage] =
      psMessages.filter({
        msg: ControlMessage =>
          msg.getRequest match {
            case Some(op: Int) => if (msg.getWorkerID == 0 && op == 1) true else false
            case None => false
          }
      }).flatMap(
        new RichFlatMapFunction[ControlMessage, ControlMessage] {

          var counter: Int = 0

          override def flatMap(message: ControlMessage, collector: Collector[ControlMessage]): Unit = {

            if (counter == 5) {
              message.getParameters match {
                case _: Option[ParameterDescriptor] =>
                  for (i <- 0 until getRuntimeContext.getExecutionConfig.getParallelism) {
                    message.setWorkerID(i)
                    collector.collect(message)
                  }
                case _ => println("Something went wrong while updating the predictors")
              }
              counter = 0
            } else counter += 1

          }
        }
      ).name("ModelUpdates")

    /** Partitioning the prediction data along with the control messages to the predictors */
    val predictionDataBlocks: ConnectedStreams[DataInstance, ControlMessage] = forecastingSource
      .connect(validRequest
        .filter(x => x.container.get.request != "Query")
        .partitionCustom(random_partitioner, (x: ControlMessage) => x.workerID)
        .union(
          modelUpdates.partitionCustom(random_partitioner, (x: ControlMessage) => x.workerID)
        ))

    /** The parallel prediction procedure happens here. */
    val predictionStream: DataStream[Prediction] = predictionDataBlocks
      .process(new Predictor[MLNodeGenerator])
      .name("Predictor")


    //////////////////////////////////////////////// Sinks /////////////////////////////////////////////////////////////


    /** A Kafka sink for the predictions. */
    predictionStream
      .map(x => x.toString)
      .addSink(new FlinkKafkaProducer[String](
        params.get("predictionsAddr", "localhost:9092"), // broker list
        params.get("predictionsTopic", "predictions"), // target topic
        new SimpleStringSchema()))
      .name("PredictionsSink")

    /** A Kafka Sink for the query responses. */
    worker.getSideOutput(queryResponse)
      .map(x => x.toString)
      .addSink(new FlinkKafkaProducer[String](
        params.get("responsesAddr", "localhost:9092"), // broker list
        params.get("responsesTopic", "responses"), // target topic
        new SimpleStringSchema()))
      .name("ResponsesSink")


    //////////////////////////////////////////// Execute OML Job ///////////////////////////////////////////////////////


    /** execute program */
    env.execute(params.get("jobName", utils.DefaultJobParameters.defaultJobName))
  }


}
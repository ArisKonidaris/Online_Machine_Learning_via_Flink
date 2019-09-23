/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package INFORE

import java.util.Properties

import INFORE.logic.{ParameterServerLogic, workerLogic}
import INFORE.message.{DataPoint, LearningMessage, psMessage}
import INFORE.parameters.{LearningParameters, LinearModelParameters}
import INFORE.utils.partitioners.random_partitioner
import breeze.linalg.{DenseVector => BreezeDenseVector}
import org.apache.flink.api.common.serialization.SimpleStringSchema
import org.apache.flink.api.java.utils.ParameterTool
import org.apache.flink.streaming.api.scala._
import org.apache.flink.ml.common._
import org.apache.flink.ml.math.DenseVector
import org.apache.flink.streaming.api.TimeCharacteristic
import org.apache.flink.streaming.connectors.fs.bucketing.BucketingSink
import org.apache.flink.streaming.connectors.kafka.{FlinkKafkaConsumer, FlinkKafkaProducer}


/**
  * Skeleton for Online Machine Learning Flink Streaming Job.
  */
object StreamingJob {
  def main(args: Array[String]) {

    /** Kafka Iteration */

    /** Default Job Parameters */
    val defaultParallelism: String = "8"
    val defaultInputFile: String = "/home/aris/IdeaProjects/DataStream/lin_class_mil.txt"
    //    val defaultInputFile: String = "hdfs://clu01.softnet.tuc.gr:8020/user/vkonidaris/lin_class_mil_e10.txt"
    val defaultOutputFile: String = "/home/aris/IdeaProjects/oml1.2/output.txt"
    //    val defaultHdfsOut: String = "hdfs://clu01.softnet.tuc.gr:8020/user/vkonidaris/output.txt"

    /** Set up the streaming execution environment */
    val env = StreamExecutionEnvironment.getExecutionEnvironment
    val params: ParameterTool = ParameterTool.fromArgs(args)

    env.getConfig.setGlobalJobParameters(params)
    env.setParallelism(params.get("k", defaultParallelism).toInt)
    //    env.setStateBackend(new FsStateBackend(params.get("stateBackend", "/home/aris/IdeaProjects/oml1.2/checkpoints")))
    //    env.setStreamTimeCharacteristic(TimeCharacteristic.EventTime)
    //    env.enableCheckpointing(params.get("checkInterval", "15000").toInt)



    /** Properties of Kafka */
    val properties = new Properties()
    properties.setProperty("bootstrap.servers", params.get("kafkaConsAddr", "localhost:9092"))


    /** The parameter server messages */
    val psMessages = env
      .addSource(new FlinkKafkaConsumer[String]("psMessages",
        new SimpleStringSchema(),
        properties)
        .setStartFromLatest()
      )

    val psMParsed: DataStream[LearningMessage] = psMessages
      .map(
        line => {
          line.slice(line.indexOf(",") + 1, line.indexOf("(")) match {
            case "LinearModelParameters" =>
              val weights: Array[Double] = line.slice(line.indexOf(".") - 1, line.indexOf(")"))
                .split(",")
                .map(_.toDouble)
              val intercept: Double = line.slice(line.indexOf(")") + 2, line.length - 1).toDouble
              psMessage(line.slice(0, line.indexOf(",")).toInt,
                LinearModelParameters(BreezeDenseVector[Double](weights), intercept))
          }
        }
      )


    /** The incoming data */
    val data = env.addSource(new FlinkKafkaConsumer[String]("data",
      new SimpleStringSchema(),
      properties)
      .setStartFromLatest()
    )
    //    val data = env.readTextFile(params.get("input", defaultInputFile))

    val parsed_data: DataStream[LearningMessage] = data
      .map(
        line => {
          val data = line.split(",").map(_.toDouble)
          val last_index = data.length - 1
          val elem = LabeledVector(data(last_index), DenseVector(data.slice(0, last_index)))
          val blockID = elem.hashCode() % params.get("k",defaultParallelism).toInt
          DataPoint(if (blockID < 0) blockID + params.get("k", defaultParallelism).toInt else blockID, elem)
        }
      )


    /** Partitioning the data to the workers */
    val data_blocks: DataStream[LearningMessage] = parsed_data.union(psMParsed)
      .partitionCustom(random_partitioner, (x: LearningMessage) => x.partition)


    /** The parallel learning procedure happens here */
    val worker: DataStream[(Int, Int, LearningParameters)] = data_blocks.flatMap(new workerLogic())


    /** The coordinator logic, where the learners are merged */
    val coordinator: DataStream[String] = worker
      .keyBy(0)
      .flatMap(new ParameterServerLogic(params.get("k", defaultParallelism).toInt))


    /** Output stream to file for debugging */
    coordinator.writeAsText(params.get("output", defaultOutputFile))
    //    coordinator.addSink(new BucketingSink[String](params.get("hdsfOut", defaultHdfsOut)))

    /** The Kafka iteration for emulating parameter server messages */
    coordinator
      .filter(x => x.contains(","))
      .addSink(new FlinkKafkaProducer[String](
        params.get("brokerList", params.get("brokerList", "localhost:9092")), // broker list
        "psMessages", // target topic
        new SimpleStringSchema) // serialization schema
      )


    /** execute program */
    env.execute("Flink Streaming Scala API Skeleton")
  }

}
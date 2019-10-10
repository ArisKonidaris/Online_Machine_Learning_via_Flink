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
import INFORE.message.{DataPoint, LearningMessage}
import INFORE.parameters.LearningParameters
import INFORE.utils.partitioners.random_partitioner
import breeze.linalg.{DenseVector => BreezeDenseVector}
import org.apache.flink.api.common.serialization.SimpleStringSchema
import org.apache.flink.api.java.utils.ParameterTool
import org.apache.flink.streaming.api.scala.{DataStream, _}
import org.apache.flink.ml.common._
import org.apache.flink.ml.math.DenseVector
import org.apache.flink.streaming.api.TimeCharacteristic
import org.apache.flink.runtime.state.filesystem.FsStateBackend
import org.apache.flink.streaming.connectors.kafka.{FlinkKafkaConsumer, FlinkKafkaProducer}


/**
  * Skeleton for Online Machine Learning Flink Streaming Job.
  */
object StreamingJob {
  def main(args: Array[String]) {

    /** Flink Iteration */

    /** Default Job Parameters */
    val defaultJobName: String = "OML_job_1"
    val defaultParallelism: String = "32"
//    val defaultInputFile: String = "hdfs://clu01.softnet.tuc.gr:8020/user/vkonidaris/lin_class_mil_e10.txt"
//    val defaultOutputFile: String = "hdfs://clu01.softnet.tuc.gr:8020/user/vkonidaris/output"
    //    val defaultStateBackend: String = "file:///home/aris/IdeaProjects/oml1.2/checkpoints"
    //      val defaultStateBackend: String = "hdfs://clu01.softnet.tuc.gr:8020/user/vkonidaris/checkpoints"

    /** Set up the streaming execution environment */
    val env = StreamExecutionEnvironment.getExecutionEnvironment
    val params: ParameterTool = ParameterTool.fromArgs(args)
    env.getConfig.setGlobalJobParameters(params)
    env.setParallelism(params.get("k", defaultParallelism).toInt)
    //    env.setStateBackend(new FsStateBackend(params.get("stateBackend", defaultStateBackend)))
//    env.setStreamTimeCharacteristic(TimeCharacteristic.EventTime)
    //    env.enableCheckpointing(params.get("checkInterval", "1000").toInt)


    /** Properties of Kafka */
    val properties = new Properties()
    properties.setProperty("bootstrap.servers", params.get("dataCons", "localhost:9092"))

    def stepFunc(data: DataStream[LearningMessage]): (DataStream[LearningMessage], DataStream[String]) = {

      /** Partitioning the data to the workers */
      val data_blocks = data.partitionCustom(random_partitioner, (x: LearningMessage) => x.partition)

      /** The parallel learning procedure happens here */
      val worker: DataStream[(Int, Int, LearningParameters)] = data_blocks.flatMap(new workerLogic)
      //      val worker: DataStream[(Int, Int, LearningParameters)] = data_blocks.flatMap(new CheckWorker)

      /** The coordinator logic, where the learners are merged */
      val coordinator: DataStream[LearningMessage] = worker
        .keyBy(0)
        .flatMap(new ParameterServerLogic)

      (coordinator, coordinator.map(x => x.toString))
    }

    /** The incoming data */
    val data = env.addSource(new FlinkKafkaConsumer[String]("data",
      new SimpleStringSchema(),
      properties)
      .setStartFromLatest()
    )
//    val data = env.readTextFile(params.get("input", defaultInputFile))

    val dataPoints: DataStream[LearningMessage] = data
      .map(
        line => {
          line.slice(line.indexOf(",") + 1, line.indexOf("(")) match {
            case "" =>
              val data = line.split(",").map(_.toDouble)
              val last_index = data.length - 1
              val elem = LabeledVector(data(last_index), DenseVector(data.slice(0, last_index)))
              val blockID = elem.hashCode() % params.get("k", defaultParallelism).toInt
              DataPoint(if (blockID < 0) blockID + params.get("k", defaultParallelism).toInt else blockID, elem)
          }
        }
      )

    val iteration = dataPoints.iterate[String]((x: DataStream[LearningMessage]) => stepFunc(x))

    /** Output stream to file for debugging */
    //    iteration.writeAsText(params.get("output", defaultOutputFile))

    iteration.map(x => System.nanoTime + " , " + x)
      .addSink(new FlinkKafkaProducer[String](
        params.get("brokerList", "localhost:9092"), // broker list
        "psMessagesStr", // target topic
        new SimpleStringSchema())
      )

    /** execute program */
    env.execute("Flink Streaming Scala API Skeleton")
  }

}
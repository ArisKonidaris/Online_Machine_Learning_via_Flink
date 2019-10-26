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

package OML

import java.util.Properties

import OML.utils.partitioners.random_partitioner
import org.apache.flink.runtime.state.filesystem.FsStateBackend


import OML.common.LabeledPoint
import OML.learners.classification._
import OML.learners.regression._
import OML.message.{DataPoint, LearningMessage}
import OML.parameters.LearningParameters
import OML.protocol.AsynchronousProto
import org.apache.flink.api.common.serialization.{SimpleStringSchema, TypeInformationSerializationSchema}
import org.apache.flink.api.java.utils.ParameterTool
import org.apache.flink.streaming.api.scala._
import org.apache.flink.ml.math.DenseVector
import org.apache.flink.streaming.api.TimeCharacteristic
import org.apache.flink.streaming.connectors.kafka.{FlinkKafkaConsumer, FlinkKafkaProducer}


/**
  * Skeleton for Online Machine Learning Flink Streaming Job.
  */
object StreamingJob {
  def main(args: Array[String]) {

    /** Kafka Iteration */

    val proto_factory: AsynchronousProto[PA] = AsynchronousProto[PA]()

    /** Default Job Parameters */
    val defaultJobName: String = "OML_job_1"
    val defaultParallelism: String = "36"
    //    val defaultInputFile: String = "hdfs://clu01.softnet.tuc.gr:8020/user/vkonidaris/lin_class_mil_e10.txt"
    //    val defaultOutputFile: String = "hdfs://clu01.softnet.tuc.gr:8020/user/vkonidaris/output"
    val defaultStateBackend: String = "file:///home/aris/IdeaProjects/oml1.2/checkpoints"
    //    val defaultStateBackend: String = "hdfs://clu01.softnet.tuc.gr:8020/user/vkonidaris/checkpoints"


    /** Set up the streaming execution environment */
    val env = StreamExecutionEnvironment.getExecutionEnvironment
    val params: ParameterTool = ParameterTool.fromArgs(args)

    env.getConfig.setGlobalJobParameters(params)
    env.setParallelism(params.get("k", defaultParallelism).toInt)
    env.setStreamTimeCharacteristic(TimeCharacteristic.EventTime)
    env.enableCheckpointing(params.get("checkInterval", "5000").toInt)
    env.setStateBackend(new FsStateBackend(params.get("stateBackend", defaultStateBackend)))

    /** The parameter server messages */
    val propertiesPS = new Properties()
    propertiesPS.setProperty("bootstrap.servers", params.get("psMessageAddress", "localhost:9092"))

    val psMessages: DataStream[LearningMessage] = env
      .addSource(new FlinkKafkaConsumer[LearningMessage]("psMessages",
        new TypeInformationSerializationSchema(createTypeInformation[LearningMessage], env.getConfig),
        propertiesPS)
        .setStartFromLatest()
      )


    /** The incoming data */
    val propertiesDt = new Properties()
    propertiesDt.setProperty("bootstrap.servers", params.get("dataCons", "localhost:9092"))
    val data = env.addSource(new FlinkKafkaConsumer[String]("data",
      new SimpleStringSchema(),
      propertiesDt)
      .setStartFromLatest()
    )
    //    val data = env.readTextFile(params.get("input", defaultInputFile))

    val parsed_data: DataStream[LearningMessage] = data
      .map(
        line => {
          val data = line.split(",").map(_.toDouble)
          val last_index = data.length - 1
          val elem = LabeledPoint(data(last_index), DenseVector(data.slice(0, last_index)))
          val blockID = elem.hashCode() % params.get("k", defaultParallelism).toInt
          DataPoint(if (blockID < 0) blockID + params.get("k", defaultParallelism).toInt else blockID, elem)
        }
      )


    /** Partitioning the data to the workers */
    val data_blocks: DataStream[LearningMessage] = parsed_data.union(psMessages)
      .partitionCustom(random_partitioner, (x: LearningMessage) => x.partition)
    //      .keyBy((x: LearningMessage) => x.partition)


    /** The parallel learning procedure happens here */
    val worker: DataStream[(Int, Int, LearningParameters)] = data_blocks.flatMap(proto_factory.workerLogic)

    /** The coordinator logic, where the learners are merged */
    val coordinator: DataStream[LearningMessage] = worker
      .keyBy(0)
      .flatMap(proto_factory.psLogic)


    /** Output stream to file for debugging */
//    coordinator.writeAsText(params.get("output", defaultOutputFile))


    /** The Kafka iteration for emulating parameter server messages */
    coordinator
      .addSink(new FlinkKafkaProducer[LearningMessage](
        params.get("psMessageAddress", "localhost:9092"), // broker list
        "psMessages", // target topic
        new TypeInformationSerializationSchema(createTypeInformation[LearningMessage], env.getConfig))
      )

    coordinator
      .map(x => System.nanoTime + " , " + x.toString)
      .addSink(new FlinkKafkaProducer[String](
        params.get("brokerList", "localhost:9092"), // broker list
        "psMessagesStr", // target topic
        new SimpleStringSchema())
      )


    /** execute program */
    env.execute(params.get("jobName", defaultJobName))
  }

}
package oml.FlinkBipartiteAPI.utils

import java.util.Properties

import org.apache.flink.api.common.serialization.{SimpleStringSchema, TypeInformationSerializationSchema}
import org.apache.flink.api.common.typeinfo.TypeInformation
import org.apache.flink.api.java.utils.ParameterTool
import org.apache.flink.streaming.api.scala._
import org.apache.flink.streaming.connectors.kafka.{FlinkKafkaConsumer, FlinkKafkaConsumerBase, FlinkKafkaProducer}

object KafkaUtils {

  def KafkaStringConsumer(topic: String)
                         (implicit params: ParameterTool, env: StreamExecutionEnvironment)
  : FlinkKafkaConsumerBase[String] = {
    new FlinkKafkaConsumer[String](topic,
      new SimpleStringSchema(),
      createProperties(topic + "Addr", topic + "Consumer"))
      .setStartFromEarliest()
  }

  def KafkaTypeConsumer[T: TypeInformation](topic: String)
                                           (implicit params: ParameterTool, env: StreamExecutionEnvironment)
  : FlinkKafkaConsumerBase[T] = {
    new FlinkKafkaConsumer[T](topic,
      new TypeInformationSerializationSchema(createTypeInformation[T], env.getConfig),
      createProperties(topic + "Addr", topic + "Consumer"))
      .setStartFromLatest()
  }

  def kafkaStringProducer(topic: String)
                         (implicit params: ParameterTool, env: StreamExecutionEnvironment)
  : FlinkKafkaProducer[String] = {
    new FlinkKafkaProducer[String](params.get(topic + "Addr", "localhost:9092"), // broker list
      topic, // target topic
      new SimpleStringSchema())
  }

  def kafkaTypeProducer[T: TypeInformation](topic: String)
                                           (implicit params: ParameterTool, env: StreamExecutionEnvironment)
  : FlinkKafkaProducer[T] = {
    new FlinkKafkaProducer[T](params.get(topic + "Addr", "localhost:9092"), // broker list
      topic, // target topic
      new TypeInformationSerializationSchema(createTypeInformation[T], env.getConfig))
  }

  def createProperties(brokerList: String, group_id: String)(implicit params: ParameterTool): Properties = {
    val properties: Properties = new Properties()
    properties.setProperty("bootstrap.servers", params.get(brokerList, "localhost:9092"))
    properties.setProperty("group.flink_worker_id", group_id)
    properties
  }

}

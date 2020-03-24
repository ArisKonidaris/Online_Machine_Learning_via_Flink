package oml.FlinkBipartiteAPI.utils.deserializers

import java.nio.ByteBuffer

import oml.FlinkBipartiteAPI.POJOs.Request
import org.apache.flink.api.common.typeinfo.TypeInformation
import org.apache.flink.streaming.connectors.kafka.KafkaDeserializationSchema
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.ObjectMapper
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.flink.api.java.typeutils.TypeExtractor.getForClass


class RequestDeserializer(val includeMetadata: Boolean) extends KafkaDeserializationSchema[Request] {

  private var mapper: ObjectMapper = _

  @throws[Exception]
  override def deserialize(record: ConsumerRecord[Array[Byte], Array[Byte]]): Request = {
    if (mapper == null) mapper = new ObjectMapper()
    var node: Request = null
    try {
      if (record.value != null) {
        node = mapper.readValue(record.value(), classOf[Request])
        if (node.isValid) {
          if (includeMetadata)
            node.setMetadata(record.topic,
              record.partition,
              ByteBuffer.wrap(record.key()).getLong,
              record.offset(),
              record.timestamp()
            )
        } else {
          node = null
        }
      }
    } catch {
      case _: Throwable => node = null
    }
    node
  }

  override def isEndOfStream(nextElement: Request) = false

  override def getProducedType: TypeInformation[Request] = getForClass(classOf[Request])
}
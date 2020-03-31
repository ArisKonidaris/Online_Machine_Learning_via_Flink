package oml.FlinkBipartiteAPI.utils.deserializers

import java.nio.ByteBuffer

import oml.FlinkBipartiteAPI.POJOs.Request
import org.apache.flink.api.common.typeinfo.TypeInformation
import org.apache.flink.api.java.typeutils.TypeExtractor.getForClass
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.ObjectMapper
import org.apache.flink.streaming.connectors.kafka.KafkaDeserializationSchema
import org.apache.kafka.clients.consumer.ConsumerRecord

class RequestDeserializer(val includeMetadata: Boolean) extends KafkaDeserializationSchema[Request]  {

  private var mapper: ObjectMapper = _

  override def isEndOfStream(request: Request): Boolean = false

  override def deserialize(record: ConsumerRecord[Array[Byte], Array[Byte]]): Request = {
    if (mapper == null) mapper = new ObjectMapper()
    var serializedObject: Request = null
    try {
      if (record.value != null) {
        serializedObject = mapper.readValue(record.value(), serializedObject.getClass)
        if (serializedObject.isValid) {
          if (includeMetadata)
            serializedObject.setMetadata(record.topic,
              record.partition,
              ByteBuffer.wrap(record.key()).getLong,
              record.offset(),
              record.timestamp()
            )
        } else serializedObject = null
      }
    } catch {
      case _: Throwable => serializedObject = null
    }
    serializedObject
  }

  override def getProducedType: TypeInformation[Request] = {
    getForClass(classOf[Request])
  }
}

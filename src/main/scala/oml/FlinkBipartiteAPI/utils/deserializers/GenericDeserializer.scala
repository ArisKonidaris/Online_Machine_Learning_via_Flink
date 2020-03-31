package oml.FlinkBipartiteAPI.utils.deserializers

import java.nio.ByteBuffer

import oml.FlinkBipartiteAPI.POJOs.Validatable
import org.apache.flink.api.common.typeinfo.TypeInformation
import org.apache.flink.api.java.typeutils.TypeExtractor.getForClass
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.ObjectMapper
import org.apache.flink.streaming.connectors.kafka.KafkaDeserializationSchema
import org.apache.kafka.clients.consumer.ConsumerRecord

class GenericDeserializer[T <: Validatable](val includeMetadata: Boolean)
  extends KafkaDeserializationSchema[T]  {

  private var mapper: ObjectMapper = _

  override def isEndOfStream(t: T): Boolean = false

  override def deserialize(record: ConsumerRecord[Array[Byte], Array[Byte]]): T = {
    if (mapper == null) mapper = new ObjectMapper()
    var serializedObject: T = null.asInstanceOf[T]
    try {
      if (record.value != null) {
        serializedObject = mapper.readValue(record.value(), classOf[T])
        if (serializedObject.isValid) {
          if (includeMetadata)
            serializedObject.setMetadata(record.topic,
              record.partition,
              ByteBuffer.wrap(record.key()).getLong,
              record.offset(),
              record.timestamp()
            )
        } else serializedObject = null.asInstanceOf[T]
      }
    } catch {
      case _: Throwable => serializedObject = null.asInstanceOf[T]
    }
    serializedObject
  }

  override def getProducedType: TypeInformation[T] = getForClass(classOf[T])
}

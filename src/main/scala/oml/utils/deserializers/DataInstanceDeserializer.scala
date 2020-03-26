package oml.utils.deserializers

import java.nio.ByteBuffer

import oml.POJOs.DataInstance
import org.apache.flink.api.common.typeinfo.TypeInformation
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.ObjectMapper
import org.apache.flink.streaming.connectors.kafka.KafkaDeserializationSchema
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.flink.api.java.typeutils.TypeExtractor.getForClass

class DataInstanceDeserializer(val includeMetadata: Boolean) extends KafkaDeserializationSchema[DataInstance] {

  private var mapper: ObjectMapper = _

  override def isEndOfStream(t: DataInstance): Boolean = false

  override def deserialize(record: ConsumerRecord[Array[Byte], Array[Byte]]): DataInstance = {
    if (mapper == null) mapper = new ObjectMapper()
    var dataPoint: DataInstance = null
    try {
      if (record.value != null) {
        dataPoint = mapper.readValue(record.value(), classOf[DataInstance])
        if (dataPoint.isValid) {
          if (includeMetadata)
            dataPoint.setMetadata(record.topic,
              record.partition,
              ByteBuffer.wrap(record.key()).getLong,
              record.offset(),
              record.timestamp()
            )
        } else dataPoint = null
      }
    } catch {
      case _: Throwable => dataPoint = null
    }
    dataPoint
  }

  override def getProducedType: TypeInformation[DataInstance] = getForClass(classOf[DataInstance])

}

package oml.FlinkBipartiteAPI.utils.serializers

import com.fasterxml.jackson.databind.ObjectMapper
import org.apache.flink.api.common.serialization.SerializationSchema

class GenericSerializer[T]()  extends SerializationSchema[T] {

  override def serialize(tuple: T): Array[Byte] = {
    val objectMapper = new ObjectMapper
    objectMapper.writeValueAsBytes(tuple)
  }

}

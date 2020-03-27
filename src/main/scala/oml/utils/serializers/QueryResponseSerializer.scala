package oml.utils.serializers

import com.fasterxml.jackson.databind.ObjectMapper
import oml.POJOs.QueryResponse
import org.apache.flink.api.common.serialization.SerializationSchema

class QueryResponseSerializer extends SerializationSchema[QueryResponse] {

  override def serialize(queryResponse: QueryResponse): Array[Byte] = {
    val objectMapper = new ObjectMapper
    objectMapper.writeValueAsBytes(queryResponse)
  }

}

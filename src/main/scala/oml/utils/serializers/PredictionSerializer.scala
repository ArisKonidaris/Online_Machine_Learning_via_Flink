package oml.utils.serializers

import com.fasterxml.jackson.databind.ObjectMapper
import oml.POJOs.Prediction
import org.apache.flink.api.common.serialization.SerializationSchema


class PredictionSerializer extends SerializationSchema[Prediction] {

  override def serialize(prediction: Prediction): Array[Byte] = {
    val objectMapper = new ObjectMapper
    objectMapper.writeValueAsBytes(prediction)
  }

}

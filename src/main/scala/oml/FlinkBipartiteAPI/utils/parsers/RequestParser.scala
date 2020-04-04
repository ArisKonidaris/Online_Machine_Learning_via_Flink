package oml.FlinkBipartiteAPI.utils.parsers

import oml.FlinkBipartiteAPI.POJOs.Request
import org.apache.flink.api.common.functions.RichFlatMapFunction
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.ObjectMapper
import org.apache.flink.util.Collector

case class RequestParser() extends RichFlatMapFunction[String, Request] {

  private val mapper: ObjectMapper = new ObjectMapper()

  override def flatMap(record: String, collector: Collector[Request]): Unit = {
    val request = mapper.readValue(record, classOf[Request])
    if (request.isValid) collector.collect(request)
  }

}

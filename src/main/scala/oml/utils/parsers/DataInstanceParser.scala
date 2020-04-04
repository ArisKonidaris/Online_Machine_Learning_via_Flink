package oml.utils.parsers

import oml.POJOs.DataInstance
import org.apache.flink.api.common.functions.RichFlatMapFunction
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.ObjectMapper
import org.apache.flink.util.Collector

case class DataInstanceParser() extends RichFlatMapFunction[String, DataInstance] {

  private val mapper: ObjectMapper = new ObjectMapper()

  override def flatMap(record: String, collector: Collector[DataInstance]): Unit = {
    val dataInstance = mapper.readValue(record, classOf[DataInstance])
    if (dataInstance.isValid) collector.collect(dataInstance)
  }

}

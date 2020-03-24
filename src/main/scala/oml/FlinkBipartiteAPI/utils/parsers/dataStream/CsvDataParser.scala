package oml.FlinkBipartiteAPI.utils.parsers.dataStream

import oml.mlAPI.math.{DenseVector, LabeledPoint}
import oml.FlinkBipartiteAPI.messages.DataPoint
import oml.FlinkBipartiteAPI.utils.parsers.StringToArrayDoublesParser
import org.apache.flink.api.common.functions.RichFlatMapFunction
import org.apache.flink.configuration.Configuration
import org.apache.flink.util.Collector

import scala.collection.mutable
import scala.util.Random

class CsvDataParser() extends RichFlatMapFunction[String, DataPoint]
  with MLParser {

  var keyMapper: mutable.Map[String, Array[String]] = _
  var r: Random = _

  override def flatMap(input: String, collector: Collector[DataPoint]): Unit = {
    val data = StringToArrayDoublesParser.parse(input)
    val last_index = data.length - 1
    val elem = LabeledPoint(data(last_index), DenseVector(data.slice(0, last_index)))
    collector.collect(DataPoint(elem))
  }

  override def open(parameters: Configuration): Unit = {}

}

package OML.utils.parsers

import OML.common.LabeledPoint
import OML.message.DataPoint
import org.apache.flink.api.common.functions.RichFlatMapFunction
import org.apache.flink.configuration.Configuration
import org.apache.flink.ml.math.DenseVector
import org.apache.flink.util.Collector

import scala.collection.mutable
import scala.util.Random

class CsvDataParser(var workers: Int) extends RichFlatMapFunction[String, DataPoint]
  with MLParser {

  var keyMapper: mutable.Map[String, Array[String]] = _
  var r: Random = _

  override def flatMap(input: String, collector: Collector[DataPoint]): Unit = {
    val data = StringToArrayDoublesParser.parse(input)
    //    val data = input.split(",").map(_.toDouble)
    val last_index = data.length - 1
    val elem = LabeledPoint(data(last_index), DenseVector(data.slice(0, last_index)))
    val blockID = elem.hashCode() % workers
    collector.collect(DataPoint(if (blockID < 0) blockID + workers.toInt else blockID, elem))
  }

  override def open(parameters: Configuration): Unit = {
    println("##############################")
    println(getRuntimeContext.getExecutionConfig.getMaxParallelism)
    println(getRuntimeContext.getExecutionConfig.getParallelism)
    println("##############################")
    //    keyMapper = getRuntimeContext.getBroadcastVariable("keyMapper").get(0)
    //
    //    if(r == null){
    //      r = new Random(100 ^ getRuntimeContext.getIndexOfThisSubtask)
    //    }
  }

}

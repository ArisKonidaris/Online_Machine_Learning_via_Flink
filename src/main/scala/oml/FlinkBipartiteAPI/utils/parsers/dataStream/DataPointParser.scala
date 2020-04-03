package oml.FlinkBipartiteAPI.utils.parsers.dataStream

import oml.FlinkBipartiteAPI.POJOs.DataInstance
import oml.mlAPI.math.{DenseVector, LabeledPoint, Point, UnlabeledPoint}
import org.apache.flink.api.common.functions.RichFlatMapFunction
import org.apache.flink.configuration.Configuration
import org.apache.flink.util.Collector

import scala.collection.JavaConverters._

/**
  * Converts a [[DataInstance]] object to a [[Point]] object.
  */
class DataPointParser() extends RichFlatMapFunction[DataInstance, Point] {

  override def flatMap(input: DataInstance, collector: Collector[Point]): Unit = {

    //TODO: Remove this line after the implementation of ml methods that use Discrete Features.
    if (input.getNumericFeatures == null || input.getDiscreteFeatures != null) return

    {
      if (input.getOperation.equals("training")) {
        val numericData = DenseVector(input.getNumericFeatures.asInstanceOf[java.util.List[Double]].asScala.toArray)
        if (input.getTarget != null)
          Some(LabeledPoint(input.getTarget, numericData))
        else
          Some(UnlabeledPoint(numericData))
      } else None
    } match {
      case Some(point: Point) => collector.collect(point)
      case _ => println("Unknown DataInstance type")
    }

  }

  override def open(parameters: Configuration): Unit = {}

}

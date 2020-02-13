package oml.mlAPI.preprocessing

import breeze.linalg.{DenseVector => BreezeDenseVector}
import breeze.numerics.sqrt
import oml.math.Breeze._
import oml.math.{LabeledPoint, Point, UnlabeledPoint, Vector}
import oml.utils.parsers.StringToArrayDoublesParser

import scala.collection.mutable
import scala.collection.mutable.ListBuffer

case class StandardScaler() extends learningPreprocessor {

  private var mean: BreezeDenseVector[Double] = _
  private var variance: BreezeDenseVector[Double] = _
  private var count: Int = _

  override def init(point: Point): Unit = {
    mean = BreezeDenseVector.zeros[Double](point.vector.size)
    variance = BreezeDenseVector.zeros[Double](point.vector.size)
    count = 0
  }

  override def fit(point: Point): Unit = {
    try {
      count += 1
      val newMean = mean + (1 / (1.0 * count)) * (point.vector.asBreeze - mean)
      variance += (point.vector.asBreeze - newMean) * (point.vector.asBreeze - mean)
      mean = newMean
    } catch {
      case _: Throwable =>
        init(point)
        fit(point)
    }
  }

  override def fit(dataSet: ListBuffer[Point]): Unit = for (point <- dataSet) if (count < Int.MaxValue) fit(point)

  override def transform(point: Point): Point = {
    if (isLearning) if (count == Int.MaxValue) freezeLearning() else fit(point)
    matchTransform(point)
  }

  override def transform(dataSet: ListBuffer[Point]): ListBuffer[Point] = {
    if (isLearning) if (count == Int.MaxValue) freezeLearning() else fit(dataSet)
    val transformedBuffer = ListBuffer[Point]()
    for (point <- dataSet) transformedBuffer.append(matchTransform(point))
    transformedBuffer
  }

  private def matchTransform(point: Point): Point = {
    point match {
      case UnlabeledPoint(_) =>
        if (count > 1) UnlabeledPoint(scale(point)) else point

      case LabeledPoint(label, _) =>
        if (count > 1) LabeledPoint(label, scale(point)) else point
    }
  }

  private def scale(point: Point): Vector = {
    ((point.vector.asBreeze - mean) / sqrt((1.0 / (count - 1)) * variance)).fromBreeze
  }

  def setMean(mean: BreezeDenseVector[Double]): StandardScaler = {
    this.mean = mean
    this
  }

  def setVariance(variance: BreezeDenseVector[Double]): StandardScaler = {
    this.variance = variance
    this
  }

  def setCount(count: Int): StandardScaler = {
    this.count = count
    this
  }

  override def setParameters(parameterMap: mutable.Map[String, Any]): StandardScaler = {
    for ((parameter, value) <- parameterMap) {
      parameter match {
        case "mean" =>
          if (value.getClass == mean.getClass) {
            try {
              setMean(BreezeDenseVector(StringToArrayDoublesParser.parse(value.asInstanceOf[String])))
            } catch {
              case e: Exception =>
                println("Error while trying to update the mean vector of StandardScaler")
                e.printStackTrace()
            }
          }

        case "variance" =>
          if (value.getClass == variance.getClass) {
            try {
              setVariance(BreezeDenseVector(StringToArrayDoublesParser.parse(value.asInstanceOf[String])))
            } catch {
              case e: Exception =>
                println("Error while trying to update the variance vector of StandardScaler")
                e.printStackTrace()
            }
          }

        case "count" =>
          if (value.getClass == count.getClass) {
            try {
              setCount(value.asInstanceOf[Double].toInt)
            } catch {
              case e: Exception =>
                println("Error while trying to update the counter of StandardScaler")
                e.printStackTrace()
            }
          }

        case _ =>
      }
    }
    this
  }

  override def setHyperParameters(hyperParameterMap: mutable.Map[String, Any]): preProcessing = {
    for ((hyperparameter, value) <- hyperParameterMap) {
      hyperparameter match {
        case "learn" =>
          if (value.getClass == learnable.getClass) {
            try {
              learnable = value.asInstanceOf[Boolean]
            } catch {
              case e: Exception =>
                println("Error while trying to update the learnable flag of StandardScaler")
                e.printStackTrace()
            }
          }

        case _ =>
      }
    }
    this
  }

}

package OML.preprocessing

import OML.math.{LabeledPoint, Point, UnlabeledPoint}
import OML.math.Breeze._
import breeze.linalg.{DenseVector => BreezeDenseVector}
import breeze.numerics.sqrt

import scala.collection.mutable.ListBuffer

case class StandardScaler() extends learningPreprocessor {

  private var mean: BreezeDenseVector[Double] = _
  private var variance: BreezeDenseVector[Double] = _
  private var count: Int = 0

  override def init(point: Point): Unit = {
    mean = BreezeDenseVector.zeros[Double](point.vector.size)
    variance = BreezeDenseVector.zeros[Double](point.vector.size)
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
    if (isLearning) {
      if (count == Int.MaxValue) {
        learnable = false
      } else {
        fit(point)
      }
    }
    matchTransform(point)
  }

  override def transform(dataSet: ListBuffer[Point]): ListBuffer[Point] = {
    if (isLearning) {
      if (count == Int.MaxValue) {
        learnable = false
      } else {
        fit(dataSet)
      }
    }
    val transformedBuffer = ListBuffer[Point]()
    for (point <- dataSet) transformedBuffer.append(matchTransform(point))
    transformedBuffer
  }

  private def matchTransform(point: Point): Point = {
    point match {
      case UnlabeledPoint(_) =>
        if (count > 1) {
          UnlabeledPoint(((point.vector.asBreeze - mean) / sqrt((1.0 / (count - 1)) * variance)).fromBreeze)
        } else {
          point
        }

      case LabeledPoint(label, _) =>
        if (count > 1) {
          LabeledPoint(label, ((point.vector.asBreeze - mean) / sqrt((1.0 / (count - 1)) * variance)).fromBreeze)
        } else {
          point
        }
    }
  }

}

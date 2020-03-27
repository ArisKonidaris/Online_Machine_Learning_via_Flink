package oml.mlAPI.preprocessing

import breeze.linalg.{DenseVector => BreezeDenseVector}
import breeze.numerics.sqrt
import oml.POJOs.Preprocessor
import oml.math.Breeze._
import oml.math.{LabeledPoint, Point, UnlabeledPoint, Vector}

import scala.collection.mutable
import scala.collection.mutable.ListBuffer
import scala.collection.JavaConverters._

case class StandardScaler() extends learningPreprocessor {

  private var mean: BreezeDenseVector[Double] = _
  private var d_squared: BreezeDenseVector[Double] = _
  private var count: Long = _

  override def init(point: Point): Unit = {
    mean = BreezeDenseVector.zeros[Double](point.vector.size)
    d_squared = BreezeDenseVector.zeros[Double](point.vector.size)
    count = 0L
  }

  override def fit(point: Point): Unit = {
    try {
      count += 1
      val newMean = mean + (1 / (1.0 * count)) * (point.vector.asBreeze - mean)
      d_squared += (point.vector.asBreeze - newMean) * (point.vector.asBreeze - mean)
      mean = newMean
    } catch {
      case _: Throwable =>
        init(point)
        fit(point)
    }
  }

  override def fit(dataSet: ListBuffer[Point]): Unit = for (point <- dataSet) if (count < Long.MaxValue) fit(point)

  override def transform(point: Point): Point = {
    if (isLearning) if (count == Long.MaxValue) freezeLearning() else fit(point)
    matchTransform(point)
  }

  override def transform(dataSet: ListBuffer[Point]): ListBuffer[Point] = {
    if (isLearning) if (count == Long.MaxValue) freezeLearning() else fit(dataSet)
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
    ((point.vector.asBreeze - mean) / sqrt((1.0 / count) * d_squared)).fromBreeze
  }

  def getMean: BreezeDenseVector[Double] = mean

  def setMean(mean: BreezeDenseVector[Double]): StandardScaler = {
    this.mean = mean
    this
  }

  def getDSquared: BreezeDenseVector[Double] = d_squared

  def setDSquared(d_squared: BreezeDenseVector[Double]): StandardScaler = {
    this.d_squared = d_squared
    this
  }

  def getCount: Long = count

  def setCount(count: Long): StandardScaler = {
    this.count = count
    this
  }

  def getVariance: BreezeDenseVector[Double] =
    if (count > 0)
      (1.0 / count) * d_squared
    else
      BreezeDenseVector.zeros[Double](0)

  def getStandardDeviation: BreezeDenseVector[Double] =
    if (count > 0)
      breeze.numerics.sqrt(getVariance)
    else
      BreezeDenseVector.zeros[Double](0)

  override def setParametersFromMap(parameterMap: mutable.Map[String, AnyRef]): StandardScaler = {
    if (parameterMap.contains("variance") && !parameterMap.contains("count")) {
      println("To update the variance vector of the StandardScaler you have to also provide the counter hyper parameter.")
      return this
    }

    if (!parameterMap.contains("variance") && parameterMap.contains("count")) {
      println("To update the counter of the StandardScaler you have to also provide the variance vector hyper parameter.")
      return this
    }
    for ((parameter, value) <- parameterMap) {
      parameter match {
        case "mean" =>
          try {
            val new_mean = BreezeDenseVector[Double](value.asInstanceOf[java.util.List[Double]].asScala.toArray)
            if (mean == null || mean.size == new_mean.size)
              mean = new_mean
            else
              throw new RuntimeException("Invalid size of new mean vector for the StandardScaler.")
          } catch {
            case e: Exception =>
              println("Error while trying to update the mean vector of StandardScaler.")
              e.printStackTrace()
          }

        case "variance" =>
          try {
            val new_DSquared = BreezeDenseVector[Double](value.asInstanceOf[java.util.List[Double]].asScala.toArray)
            if (d_squared == null || d_squared.size == new_DSquared.size)
              d_squared = new_DSquared
            else
              throw new RuntimeException("Invalid size of new variance vector for the StandardScaler.")
          } catch {
            case e: Exception =>
              println("Error while trying to update the variance vector of StandardScaler.")
              e.printStackTrace()
          }

        case "count" =>
            try {
              setCount(value.asInstanceOf[Long])
              d_squared = (1.0 * count) * d_squared
            } catch {
              case e: Exception =>
                println("Error while trying to update the counter of StandardScaler.")
                e.printStackTrace()
            }

        case _ =>
      }
    }
    this
  }

  override def setHyperParametersFromMap(hyperParameterMap: mutable.Map[String, AnyRef]): preProcessing = {
    for ((hyperparameter, value) <- hyperParameterMap) {
      hyperparameter match {
        case "learn" =>
          try {
            learnable = value.asInstanceOf[Boolean]
          } catch {
            case e: Exception =>
              println("Error while trying to update the learnable flag of StandardScaler")
              e.printStackTrace()
          }

        case _ =>
      }
    }
    this
  }

  override def generatePOJOPreprocessor: Preprocessor = {
    new Preprocessor("StandardScaler",
      Map[String, AnyRef](("learn", learnable.asInstanceOf[AnyRef])).asJava,
      Map[String, AnyRef](
        ("mean", if(mean == null) null else mean.data.asInstanceOf[AnyRef]),
        ("variance", if(d_squared == null) null else getVariance.data.asInstanceOf[AnyRef]),
        ("count", count.asInstanceOf[AnyRef])
      ).asJava
    )
  }

}

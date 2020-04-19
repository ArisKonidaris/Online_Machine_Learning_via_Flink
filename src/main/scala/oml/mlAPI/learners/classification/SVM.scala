package oml.mlAPI.learners.classification

import oml.mlAPI.math.Breeze._
import oml.mlAPI.learners.{Learner, OnlineLearner}
import oml.mlAPI.math.{LabeledPoint, Point, Vector}
import oml.mlAPI.parameters.{Bucket, LearningParameters, ParameterDescriptor, VectorBias => linear_params}
import oml.mlAPI.scores.Scores
import breeze.linalg.{DenseVector => BreezeDenseVector}
import oml.FlinkBipartiteAPI.POJOs

import scala.collection.JavaConverters._
import scala.collection.mutable
import scala.collection.mutable.ListBuffer

case class SVM() extends OnlineLearner with Classifier with Serializable {

  protected var C: Double = 0.01

  protected var weights: linear_params = _

  protected var count: Long = 0L

  override def initialize_model(data: Point): Unit = {
    weights = linear_params(BreezeDenseVector.zeros[Double](data.getVector.size), 0.0)
  }

  def predictWithMargin(data: Point): Option[Double] = {
    try {
      Some((data.vector.asBreeze dot weights.weights) + weights.intercept)
    } catch {
      case _: Throwable => None
    }
  }

  override def predict(data: Point): Option[Double] = {
    predictWithMargin(data) match {
      case Some(pred) => if (pred >= 0.0) Some(1.0) else Some(-1.0)
      case None => Some(Double.MinValue)
    }
  }

  override def fit(data: Point): Unit = {
    if (count < Long.MaxValue) fitLoss(data)
    ()
  }

  override def fitLoss(data: Point): Double = {
    if (count == Long.MaxValue)
      0
    else
      predictWithMargin(data) match {
        case Some(prediction) =>
          val label: Double = createLabel(data.asInstanceOf[LabeledPoint].label)
          val sign: Double = if (label * prediction < 1.0) 1.0 else 0.0
          val loss: Double = Math.max(0.0, 1.0 - label * prediction)

          val direction = linear_params(weights.weights - C * label * data.vector.asBreeze * sign, - label * sign)

          count += 1
          weights += (direction / count)

          loss
        case None =>
          checkParameters(data)
          fitLoss(data)
      }
  }

  override def score(test_set: ListBuffer[Point]): Double =
    Scores.F1Score(test_set.asInstanceOf[ListBuffer[LabeledPoint]], this)

  private def createLabel(label: Double): Double = if (label == 0.0) -1.0 else label

  private def checkParameters(data: Point): Unit = {
    if (weights == null) {
      initialize_model(data)
    } else {
      if(weights.weights.size != data.getVector.size)
        throw new RuntimeException("Incompatible model and data point size.")
      else
        throw new RuntimeException("Something went wrong while fitting the data point " +
          data + " to learner " + this + ".")
    }
  }

  override def getParameters: Option[LearningParameters] = Some(weights)

  override def setParameters(params: LearningParameters): Learner = {
    assert(params.isInstanceOf[linear_params])
    weights = params.asInstanceOf[linear_params]
    this
  }

  def setC(c: Double): Unit = this.C = c

  def setCount(count: Long): Unit = this.count = count

  override def setParametersFromMap(parameterMap: mutable.Map[String, AnyRef]): Learner = {
    for ((parameter, value) <- parameterMap) {
      parameter match {
        case "a" =>
          try {
            val new_weights = BreezeDenseVector[Double](value.asInstanceOf[java.util.List[Double]].asScala.toArray)
            if (weights == null || weights.weights.size == new_weights.size)
              weights.weights = new_weights
            else
              throw new RuntimeException("Invalid size of new weight vector for the SVM classifier.")
          } catch {
            case e: Exception =>
              println("Error while trying to update the weights of the SVM classifier.")
              e.printStackTrace()
          }
        case "b" =>
          try {
            weights.intercept = value.asInstanceOf[Double]
          } catch {
            case e: Exception =>
              println("Error while trying to update the intercept of the SVM classifier.")
              e.printStackTrace()
          }
        case _ =>
      }
    }
    this
  }

  override def setHyperParametersFromMap(hyperParameterMap: mutable.Map[String, AnyRef]): Learner = {
    for ((hyperparameter, value) <- hyperParameterMap) {
      hyperparameter match {
        case "C" =>
          try {
            setC(value.asInstanceOf[Double])
          } catch {
            case e: Exception =>
              println("Error while trying to update the C hyper parameter of the SVM classifier.")
              e.printStackTrace()
          }
        case "count" =>
          try {
            setCount(value.asInstanceOf[Double].toLong)
          } catch {
            case e: Exception =>
              println("Error while trying to update the count hyper parameter of the SVM classifier.")
              e.printStackTrace()
          }
        case _ =>
      }
    }
    this
  }

  override def toString: String = s"SVM classifier ${this.hashCode}"

  override def generateParameters: ParameterDescriptor => LearningParameters = new linear_params().generateParameters

  override def getSerializedParams: (LearningParameters , Boolean, Bucket) => (Array[Int], Vector) =
    new linear_params().generateSerializedParams

  override def generatePOJOLearner: POJOs.Learner = {
    new POJOs.Learner("SVM",
      Map[String, AnyRef](("C", C.asInstanceOf[AnyRef])).asJava,
      Map[String, AnyRef](
        ("a", if(weights == null) null else weights.weights.data.asInstanceOf[AnyRef]),
        ("b", if(weights == null) null else weights.intercept.asInstanceOf[AnyRef])
      ).asJava
    )
  }

}

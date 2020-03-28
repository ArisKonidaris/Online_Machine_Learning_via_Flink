package oml.mlAPI.learners

import oml.mlAPI.math.Breeze._
import oml.mlAPI.math.{Point, Vector}
import oml.mlAPI.parameters.{Bucket, LearningParameters, ParameterDescriptor, LinearModelParameters => linear_params}

import breeze.linalg.{DenseVector => BreezeDenseVector}
import scala.collection.JavaConverters._
import scala.collection.mutable

abstract class PassiveAggressiveLearners extends OnlineLearner {

  protected var C: Double = 0.01

  protected var weights: linear_params = _

  override def getParameters: Option[LearningParameters] = Some(weights)

  override def setParameters(params: LearningParameters): Learner = {
    assert(params.isInstanceOf[linear_params])
    weights = params.asInstanceOf[linear_params]
    this
  }

  override def generateParameters: ParameterDescriptor => LearningParameters = new linear_params().generateParameters

  override def getSerializedParams: (LearningParameters , Boolean, Bucket) => (Array[Int], Vector, Bucket) =
    new linear_params().generateSerializedParams

  override def initialize_model(data: Point): Unit = {
    weights = linear_params(BreezeDenseVector.zeros[Double](data.vector.size), 0.0)
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
      case Some(pred) => if (pred >= 0.0) Some(1.0) else Some(0.0)
      case None => Some(Double.MinValue)
    }
  }

  def LagrangeMultiplier(loss: Double, data: Point): Double = {
    loss / (((data.vector dot data.vector) + 1.0) + 1.0 / (2.0 * C))
  }

  def setC(c: Double): PassiveAggressiveLearners = {
    this.C = c
    this
  }

  override def setParametersFromMap(parameterMap: mutable.Map[String, AnyRef]): Learner = {
    for ((parameter, value) <- parameterMap) {
      parameter match {
        case "a" =>
          try {
            val new_weights = BreezeDenseVector[Double](value.asInstanceOf[java.util.List[Double]].asScala.toArray)
            if (weights == null || weights.weights.size == new_weights.size)
              weights.weights = new_weights
            else
              throw new RuntimeException("Invalid size of new weight vector for the PA classifier")
          } catch {
            case e: Exception =>
              println("Error while trying to update the weights of the PA classifier")
              e.printStackTrace()
          }
        case "b" =>
          try {
            weights.intercept = value.asInstanceOf[Double]
          } catch {
            case e: Exception =>
              println("Error while trying to update the intercept of the PA classifier")
              e.printStackTrace()
          }
        case _ =>
      }
    }
    this
  }
}

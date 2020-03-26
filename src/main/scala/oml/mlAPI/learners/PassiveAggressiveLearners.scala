package oml.mlAPI.learners

import breeze.linalg.{DenseVector => BreezeDenseVector}
import oml.math.Breeze._
import oml.math.Point
import oml.parameters.{Bucket, LearningParameters, ParameterDescriptor, LinearModelParameters => lin_params}

import scala.collection.JavaConverters._
import scala.collection.mutable

abstract class PassiveAggressiveLearners extends OnlineLearner {

  private var C: Double = 0.01

  override def generateParameters: ParameterDescriptor => LearningParameters = new lin_params().generateParameters

  override def generateDescriptor: (LearningParameters , Boolean, Bucket) => ParameterDescriptor =
    new lin_params().generateDescriptor

  override def initialize_model(data: Point): Unit = {
    weights = lin_params(BreezeDenseVector.zeros[Double](data.vector.size), 0.0)
  }

  def predictWithMargin(data: Point): Option[Double] = {
    try {
      Some(
        (data.vector.asBreeze dot weights.asInstanceOf[lin_params].weights)
          + weights.asInstanceOf[lin_params].intercept
      )
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

  override def setParameters(parameterMap: mutable.Map[String, AnyRef]): Learner = {
    for ((parameter, value) <- parameterMap) {
      parameter match {
        case "a" =>
          try {
            val new_weights = BreezeDenseVector[Double](value.asInstanceOf[java.util.List[Double]].asScala.toArray)
            if (weights == null || weights.asInstanceOf[lin_params].weights.size == new_weights.size)
              weights.asInstanceOf[lin_params].weights = new_weights
            else
              throw new RuntimeException("Invalid size of new weight vector for the PA classifier")
          } catch {
            case e: Exception =>
              println("Error while trying to update the weights of the PA classifier")
              e.printStackTrace()
          }
        case "b" =>
          try {
            weights.asInstanceOf[lin_params].intercept = value.asInstanceOf[Double]
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

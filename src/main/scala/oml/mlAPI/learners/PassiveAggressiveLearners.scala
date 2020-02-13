package oml.mlAPI.learners

import breeze.linalg.{DenseVector => BreezeDenseVector}
import oml.math.Breeze._
import oml.math.Point
import oml.parameters.{LinearModelParameters => lin_params}
import oml.utils.parsers.StringToArrayDoublesParser

import scala.collection.mutable

abstract class PassiveAggressiveLearners extends OnlineLearner {

  private var C: Double = 0.01

  override def initialize_model(data: Point): Unit = {
    weights = lin_params(BreezeDenseVector.zeros[Double](data.vector.size), 0.0)
  }

  override def predict(data: Point): Option[Double] = {
    try {
      Some(
        (data.vector.asBreeze dot weights.asInstanceOf[lin_params].weights)
          + weights.asInstanceOf[lin_params].intercept
      )
    } catch {
      case _: Throwable => None
    }
  }

  def LagrangeMultiplier(loss: Double, data: Point): Double = {
    loss / (((data.vector dot data.vector) + 1.0) + 1.0 / (2.0 * C))
  }

  def setC(c: Double): PassiveAggressiveLearners = {
    this.C = c
    this
  }

  override def setParameters(parameterMap: mutable.Map[String, Any]): Learner = {
    for ((parameter, value) <- parameterMap) {
      parameter match {
        case "a" =>
          try {
            weights.asInstanceOf[lin_params].weights = BreezeDenseVector[Double](StringToArrayDoublesParser
              .parse(value.asInstanceOf[String]))
          } catch {
            case e: Exception =>
              println("Error while trying to update the parameters of PA learner")
              e.printStackTrace()
          }
        case "b" =>
          try {
            weights.asInstanceOf[lin_params].intercept = value.asInstanceOf[Double]
          } catch {
            case e: Exception =>
              println("Error while trying to update the intercept flag of PA learner")
              e.printStackTrace()
          }
        case _ =>
      }
    }
    this
  }
}

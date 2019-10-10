package INFORE.learners.classification

import INFORE.common.{LabeledPoint, Point}
import INFORE.learners.Learner
import INFORE.parameters.{LearningParameters => l_params, LinearModelParameters => lin_params}
import org.apache.flink.ml.math.Breeze._
import breeze.linalg.{DenseVector => BreezeDenseVector}
import org.apache.flink.api.common.state.AggregatingState

import scala.collection.mutable
import scala.collection.mutable.ListBuffer

/** Implementation of Passive Aggressive Classifier */
case class PA() extends Learner {

  private val c: Double = 0.01

  override def initialize_model(data: Point): Unit = {
    parameters = lin_params(weights = BreezeDenseVector.zeros[Double](data.vector.size), intercept = 0.0)
  }

  override def initialize_model_safe(data: Point)(implicit gModel: AggregatingState[l_params, l_params]): Unit = {
    gModel add lin_params(weights = BreezeDenseVector.zeros[Double](data.vector.size), intercept = 0.0)
  }

  override def predict(data: Point): Option[Double] = {
    try {
      Some(
        (data.vector.asBreeze dot parameters.asInstanceOf[lin_params].weights)
          + parameters.asInstanceOf[lin_params].intercept
      )
    } catch {
      case _: Throwable => None
    }
  }

  override def predict_safe(data: Point)(implicit mdl: AggregatingState[l_params, l_params]): Option[Double] = {
    try {
      Some(
        (data.vector.asBreeze dot mdl.get.asInstanceOf[lin_params].weights)
          + mdl.get.asInstanceOf[lin_params].intercept
      )
    } catch {
      case _: Throwable => None
    }
  }

  override def fit(data: Point): Unit = {
    predict(data) match {
      case Some(prediction) =>
        val label: Double = if (data.asInstanceOf[LabeledPoint].label == 0.0)
          -1.0
        else
          data.asInstanceOf[LabeledPoint].label
        val loss: Double = 1.0 - label * prediction

        if (loss > 0.0) {
          val Lagrange_Multiplier: Double = loss / (((data.vector dot data.vector) + 1.0) + 1 / (2 * c))
          parameters = parameters + lin_params(
            (data.vector.asBreeze * (Lagrange_Multiplier * label)).asInstanceOf[BreezeDenseVector[Double]],
            Lagrange_Multiplier * label)
        }
    }
  }

  override def fit_safe(data: Point)(implicit mdl: AggregatingState[l_params, l_params]): Unit = {
    predict_safe(data) match {
      case Some(prediction) =>
        val label: Double = if (data.asInstanceOf[LabeledPoint].label == 0.0)
          -1.0
        else
          data.asInstanceOf[LabeledPoint].label
        val loss: Double = 1.0 - label * prediction

        if (loss > 0.0) {
          val Lagrange_Multiplier: Double = loss / (((data.vector dot data.vector) + 1.0) + 1 / (2 * c))
          mdl add lin_params(
            (data.vector.asBreeze * (Lagrange_Multiplier * label)).asInstanceOf[BreezeDenseVector[Double]],
            Lagrange_Multiplier * label)
        }
    }
  }

  override def score(test_set: ListBuffer[Point]): Option[Double] = {
    try {
      if (test_set.nonEmpty && parameters != null) {
        Some((for (test <- test_set) yield {
          val prediction: Double = predict(test) match {
            case Some(pred) =>
              if (pred >= 0.0) 1.0 else 0.0
            case None => Double.MinValue
          }
          if (test.asInstanceOf[LabeledPoint].label == prediction) 1 else 0
        }).sum / (1.0 * test_set.length))
      } else {
        None
      }
    } catch {
      case _: Throwable => None
    }
  }

  override def score_safe(test_set: ListBuffer[Point])
                         (implicit mdl: AggregatingState[l_params, l_params]): Option[Double] = {
    try {
      if (test_set.nonEmpty && mdl.get != null) {
        Some((for (test <- test_set) yield {
          val prediction: Double = predict_safe(test) match {
            case Some(pred) =>
              if (pred >= 0.0) 1.0 else 0.0
            case None => Double.MinValue
          }
          if (test.asInstanceOf[LabeledPoint].label == prediction) 1 else 0
        }).sum / (1.0 * test_set.length))
      } else {
        None
      }
    } catch {
      case _: Throwable => None
    }
  }

  override def toString: String = s"PA classifier ${this.hashCode}"


}

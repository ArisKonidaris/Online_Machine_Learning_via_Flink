package INFORE.learners.classification

import INFORE.common.{LabeledPoint, Point}
import INFORE.learners.Learner
import INFORE.parameters.{LearningParameters => l_params, LinearModelParameters => lin_params}
import org.apache.flink.ml.math.Breeze._
import breeze.linalg.{DenseVector => BreezeDenseVector}
import org.apache.flink.api.common.state.AggregatingState

/** Implementation of Passive Aggressive Classifier */
case class PA() extends Learner {

  private val c: Double = 0.01

  override def initialize_model(data: Point)(implicit gModel: AggregatingState[l_params, l_params]): Unit = {
    gModel add lin_params(BreezeDenseVector.zeros[Double](data.vector.size), 0.0)
  }

  override def predict(data: Point)(implicit mdl: AggregatingState[l_params, l_params]): Option[Double] = {
    try {
      val parameters = mdl.get.asInstanceOf[lin_params]
      Some((data.vector.asBreeze dot parameters.weights) + parameters.intercept)
    } catch {
      case _: Throwable => None
    }
  }

  override def fit(data: Point)(implicit mdl: AggregatingState[l_params, l_params]): Unit = {
    predict(data) match {
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

  override def score(test_set: Array[Point])(implicit mdl: AggregatingState[l_params, l_params]): Option[Double] = {
    try {
      if (test_set.length > 0) {
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

  override def toString: String = s"PA classifier ${this.hashCode}"

}

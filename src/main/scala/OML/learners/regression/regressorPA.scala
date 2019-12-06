package OML.learners.regression

import OML.math.LabeledPoint
import OML.learners.Learner
import OML.parameters.{LearningParameters => l_params, LinearModelParameters => lin_params}
import OML.math.Breeze._
import OML.math.{LabeledPoint, Point}
import breeze.linalg.{DenseVector => BreezeDenseVector}
import org.apache.flink.api.common.state.AggregatingState

import scala.collection.mutable.ListBuffer

case class regressorPA() extends Learner {

  private val c: Double = 0.01
  private val epsilon: Double = 0.1

  override def initialize_model(data: Point): Unit = {
    parameters = lin_params(BreezeDenseVector.zeros[Double](data.vector.size), 0.0)
  }

  override def initialize_model_safe(data: Point)(implicit gModel: AggregatingState[l_params, l_params]): Unit = {
    gModel add lin_params(BreezeDenseVector.zeros[Double](data.vector.size), 0.0)
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
        val label: Double = data.asInstanceOf[LabeledPoint].label
        val loss: Double = Math.abs(data.asInstanceOf[LabeledPoint].label - prediction) - epsilon

        if (loss > 0.0) {
          val Lagrange_Multiplier: Double = loss / (((data.vector dot data.vector) + 1.0) + 1 / (2 * c))
          val sign: Double = if ((label - prediction) >= 0) 1.0 else -1.0
          parameters += lin_params(
            (data.vector.asBreeze * (Lagrange_Multiplier * sign)).asInstanceOf[BreezeDenseVector[Double]],
            Lagrange_Multiplier * sign)
        }

      case None =>
        if (parameters == null) initialize_model(data)
        fit(data)
    }
  }

  override def fit(batch: ListBuffer[Point]): Unit = {
    for (point <- batch) fit(point)
  }

  override def fit_safe(data: Point)(implicit mdl: AggregatingState[l_params, l_params]): Unit = {
    predict_safe(data) match {
      case Some(prediction) =>
        val label: Double = data.asInstanceOf[LabeledPoint].label
        val loss: Double = Math.abs(data.asInstanceOf[LabeledPoint].label - prediction) - epsilon

        if (loss > 0.0) {
          val Lagrange_Multiplier: Double = loss / (((data.vector dot data.vector) + 1.0) + 1 / (2 * c))
          val sign: Double = if ((label - prediction) >= 0) 1.0 else -1.0
          mdl add lin_params(
            (data.vector.asBreeze * (Lagrange_Multiplier * sign)).asInstanceOf[BreezeDenseVector[Double]],
            Lagrange_Multiplier * sign)
        }
    }
  }

  override def fit_safe(batch: ListBuffer[Point])(implicit mdl: AggregatingState[l_params, l_params]): Unit = {
    for (point <- batch) fit_safe(point)
  }

  override def score(test_set: ListBuffer[Point]): Option[Double] = {
    try {
      if (test_set.nonEmpty && parameters != null) {
        Some(
          Math.sqrt(
            (for (test <- test_set) yield {
              predict(test) match {
                case Some(pred) => Math.pow(test.asInstanceOf[LabeledPoint].label - pred, 2)
                case None => Double.MaxValue
              }
            }).sum / (1.0 * test_set.length)
          )
        )
      } else None
    } catch {
      case _: Throwable => None
    }
  }

  override def score_safe(test_set: AggregatingState[Point, Option[Point]], test_set_size: Int)
                         (implicit mdl: AggregatingState[l_params, l_params]): Option[Double] = {
    try {
      if (test_set_size > 0 && mdl.get != null) {
        val temp: ListBuffer[Point] = ListBuffer[Point]()
        val MSE: Double = (for (_ <- 0 until test_set_size)
          yield {
            val data = test_set.get.get
            temp += data
            predict_safe(data) match {
              case Some(pred) => Math.pow(data.asInstanceOf[LabeledPoint].label - pred, 2)
              case None => Double.MaxValue
            }
          }).sum / (1.0 * test_set_size)
        for (t <- temp) test_set add t
        Some(MSE)
      } else {
        None
      }
    } catch {
      case _: Throwable => None
    }
  }

  override def toString: String = s"PA regressor ${this.hashCode}"

}

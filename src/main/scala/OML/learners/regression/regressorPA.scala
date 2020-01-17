package OML.learners.regression

import OML.learners.{Learner, PassiveAggressiveLearners}
import OML.parameters.{LearningParameters => l_params, LinearModelParameters => lin_params}
import OML.math.Breeze._
import OML.math.{LabeledPoint, Point}
import breeze.linalg.{DenseVector => BreezeDenseVector}
import org.apache.flink.api.common.state.AggregatingState

import scala.collection.mutable
import scala.collection.mutable.ListBuffer

case class regressorPA() extends PassiveAggressiveLearners {

  private var epsilon: Double = 0.0

  override def fit(data: Point): Unit = {
    predict(data) match {
      case Some(prediction) =>
        val label: Double = data.asInstanceOf[LabeledPoint].label
        val loss: Double = Math.abs(label - prediction) - epsilon

        if (loss > 0.0) {
          val Lagrange_Multiplier: Double = LagrangeMultiplier(loss, data)
          val sign: Double = if ((label - prediction) >= 0) 1.0 else -1.0
          weights += lin_params(
            (data.vector.asBreeze * (Lagrange_Multiplier * sign)).asInstanceOf[BreezeDenseVector[Double]],
            Lagrange_Multiplier * sign)
        }

      case None =>
        if (weights == null) initialize_model(data)
        fit(data)
    }
  }

  override def fit_safe(data: Point)(implicit mdl: AggregatingState[l_params, l_params]): Unit = {
    predict_safe(data) match {
      case Some(prediction) =>
        val label: Double = data.asInstanceOf[LabeledPoint].label
        val loss: Double = Math.abs(label - prediction) - epsilon

        if (loss > 0.0) {
          val Lagrange_Multiplier: Double = LagrangeMultiplier(loss, data)
          val sign: Double = if ((label - prediction) >= 0) 1.0 else -1.0
          mdl add lin_params(
            (data.vector.asBreeze * (Lagrange_Multiplier * sign)).asInstanceOf[BreezeDenseVector[Double]],
            Lagrange_Multiplier * sign)
        }
    }
  }

  override def score(test_set: ListBuffer[Point]): Option[Double] = {
    try {
      if (test_set.nonEmpty && weights != null) {
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
        val RMSE: Double = Math.sqrt(
          (for (_ <- 0 until test_set_size) yield {
            val data = test_set.get.get
            temp += data
            predict_safe(data) match {
              case Some(pred) => Math.pow(data.asInstanceOf[LabeledPoint].label - pred, 2)
              case None => Double.MaxValue
            }
          }).sum / (1.0 * test_set_size))
        for (t <- temp) test_set add t
        Some(RMSE)
      } else {
        None
      }
    } catch {
      case _: Throwable => None
    }
  }

  def setEpsilon(epsilon: Double): regressorPA = {
    this.epsilon = epsilon
    this
  }

  override def setHyperParameters(hyperParameterMap: mutable.Map[String, Any]): Learner = {
    for ((hyperparameter, value) <- hyperParameterMap) {
      hyperparameter match {
        case "epsilon" =>
          try {
            setEpsilon(value.asInstanceOf[Double])
          } catch {
            case e: Exception =>
              println("Error while trying to update the epsilon hyperparameter of PA regressor")
              e.printStackTrace()
          }
        case "C" =>
          try {
            setC(value.asInstanceOf[Double])
          } catch {
            case e: Exception =>
              println("Error while trying to update the C hyperparameter of PA regressor")
              e.printStackTrace()
          }
        case _ =>
      }
    }
    this
  }

  override def toString: String = s"PA regressor ${this.hashCode}"

}

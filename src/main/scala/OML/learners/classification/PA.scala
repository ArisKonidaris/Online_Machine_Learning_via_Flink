package OML.learners.classification

import OML.common.Parameter
import OML.learners.Learner
import OML.parameters.{LearningParameters => l_params, LinearModelParameters => lin_params}
import OML.math.Breeze._
import OML.math.{LabeledPoint, Point}
import breeze.linalg.{DenseVector => BreezeDenseVector}
import org.apache.flink.api.common.state.AggregatingState

import scala.collection.mutable.ListBuffer

/** Implementation of Passive Aggressive Classifier */
case class PA() extends Learner {

  import PA._

  override def initialize_model(data: Point): Unit = {
    weights = lin_params(weights = BreezeDenseVector.zeros[Double](data.vector.size), intercept = 0.0)
  }

  override def initialize_model_safe(data: Point)(implicit gModel: AggregatingState[l_params, l_params]): Unit = {
    gModel add lin_params(weights = BreezeDenseVector.zeros[Double](data.vector.size), intercept = 0.0)
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
        val label: Double = zeroLabel(data.asInstanceOf[LabeledPoint].label)
        if (checkLabel(label)) {
          val loss: Double = 1.0 - label * prediction
          if (loss > 0.0) {
            val Lagrange_Multiplier: Double = LagrangeMultiplier(loss, data)
            weights += lin_params(
              (data.vector.asBreeze * (Lagrange_Multiplier * label)).asInstanceOf[BreezeDenseVector[Double]],
              Lagrange_Multiplier * label)
          }
        }
      case None =>
        if (weights == null) initialize_model(data)
        fit(data)
    }
  }

  override def fit(batch: ListBuffer[Point]): Unit = for (point <- batch) fit(point)

  override def fit_safe(data: Point)(implicit mdl: AggregatingState[l_params, l_params]): Unit = {
    predict_safe(data) match {
      case Some(prediction) =>
        val label: Double = zeroLabel(data.asInstanceOf[LabeledPoint].label)
        if (checkLabel(label)) {
          val loss: Double = 1.0 - label * prediction
          if (loss > 0.0) {
            val Lagrange_Multiplier: Double = LagrangeMultiplier(loss, data)
            mdl add lin_params(
              (data.vector.asBreeze * (Lagrange_Multiplier * label)).asInstanceOf[BreezeDenseVector[Double]],
              Lagrange_Multiplier * label)
          }
        }
      case None =>
    }
  }

  override def fit_safe(batch: ListBuffer[Point])(implicit mdl: AggregatingState[l_params, l_params]): Unit = {
    for (point <- batch) fit_safe(point)
  }

  override def score(test_set: ListBuffer[Point]): Option[Double] = {
    try {
      if (test_set.nonEmpty && weights != null) {
        Some((for (test <- test_set) yield {
          val prediction: Double = predict(test) match {
            case Some(pred) => if (pred >= 0.0) 1.0 else 0.0
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

  override def score_safe(test_set: AggregatingState[Point, Option[Point]], test_set_size: Int)
                         (implicit mdl: AggregatingState[l_params, l_params]): Option[Double] = {
    try {
      if (test_set_size > 0 && mdl.get != null) {
        val temp: ListBuffer[Point] = ListBuffer[Point]()
        val accuracy: Double = (for (_ <- 0 until test_set_size)
          yield {
            val data = test_set.get.get
            temp += data
            val prediction = if (predict_safe(data)(mdl).get >= 0.0) 1.0 else 0.0
            if (data.asInstanceOf[LabeledPoint].label == prediction) 1 else 0
          }).sum / (1.0 * test_set_size)
        for (t <- temp) test_set add t
        Some(accuracy)
      } else {
        None
      }
    } catch {
      case _: Throwable => None
    }
  }

  private def zeroLabel(label: Double): Double = if (label == 0.0) -1.0 else label

  private def checkLabel(label: Double): Boolean = label == 1.0 || label == -1.0

  private def LagrangeMultiplier(loss: Double, data: Point): Double = {
    loss / (((data.vector dot data.vector) + 1.0) + 1 / (2 * parameters(C)))
  }

  def setC(c: Double): PA = {
    setParameter(C, c)
    this
  }

  override def toString: String = s"PA classifier ${this.hashCode}"

}

object PA {

  // ====================================== Parameters =============================================

  case object C extends Parameter[Double] {
    override val defaultValue: Option[Double] = Some(0.01)
  }

  // =================================== Factory methods ===========================================

  def apply(): PA = {
    new PA()
  }
}

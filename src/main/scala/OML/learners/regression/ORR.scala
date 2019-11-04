package OML.learners.regression

import OML.common.{LabeledPoint, Point}
import OML.learners.Learner
import OML.parameters.{LearningParameters => l_params, MatrixLinearModelParameters => mlin_params}
import org.apache.flink.ml.math.Breeze._
import breeze.linalg._
import breeze.linalg.DenseVector
import org.apache.flink.api.common.state.AggregatingState

import scala.collection.mutable.ListBuffer

case class ORR() extends Learner {

  private val lambda: Double = 0.0

  private def model_init(n: Int): mlin_params = {
    mlin_params(lambda * diag(DenseVector.fill(n) {
      0.0
    }), DenseVector.fill(n) {
      0.0
    })
  }

  private def add_bias(data: Point): DenseVector[Double] = {
    DenseVector.vertcat(
      data.vector.asBreeze.asInstanceOf[DenseVector[Double]],
      DenseVector.ones(1))
  }

  override def initialize_model(data: Point): Unit = {
    parameters = model_init(data.vector.size + 1)
  }

  override def initialize_model_safe(data: Point)(implicit gModel: AggregatingState[l_params, l_params]): Unit = {
    gModel add model_init(data.vector.size + 1)
  }

  override def predict(data: Point): Option[Double] = {
    try {
      val x: DenseVector[Double] = add_bias(data)
      Some(parameters.asInstanceOf[mlin_params].b.t * pinv(parameters.asInstanceOf[mlin_params].A) * x)
    } catch {
      case e: Exception => e.printStackTrace()
        None
    }
  }

  override def predict_safe(data: Point)(implicit mdl: AggregatingState[l_params, l_params]): Option[Double] = {
    try {
      val x: DenseVector[Double] = add_bias(data)
      Some(mdl.get.asInstanceOf[mlin_params].b.t * pinv(mdl.get.asInstanceOf[mlin_params].A) * x)
    } catch {
      case e: Exception => e.printStackTrace()
        None
    }
  }

  override def fit(data: Point): Unit = {
    val x: DenseVector[Double] = add_bias(data)
    val a: DenseMatrix[Double] = x * x.t
    try {
      parameters += mlin_params(a, data.asInstanceOf[LabeledPoint].label * x)
    } catch {
      case _: Exception =>
        if (parameters == null) initialize_model(data)
        fit(data)
    }
  }

  override def fit(batch: ListBuffer[Point]): Unit = {
    for (point <- batch) fit(point)
  }

  override def fit_safe(data: Point)(implicit mdl: AggregatingState[l_params, l_params]): Unit = {
    val x: DenseVector[Double] = add_bias(data)
    val a: DenseMatrix[Double] = x * x.t
    mdl add mlin_params(a, data.asInstanceOf[LabeledPoint].label * x)
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

  override def score_safe(test_set: ListBuffer[Point])
                         (implicit mdl: AggregatingState[l_params, l_params]): Option[Double] = {
    try {
      if (test_set.nonEmpty && mdl.get != null) {
        Some(
          Math.sqrt(
            (for (test <- test_set) yield {
              predict_safe(test) match {
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

  override def toString: String = s"ORR ${this.hashCode}"

}

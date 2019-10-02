package INFORE.learners.regression

import INFORE.common.{LabeledPoint, Point}
import INFORE.learners.Learner
import INFORE.parameters.{LearningParameters => l_params, MatrixLinearModelParameters => mlin_params}
import org.apache.flink.ml.math.Breeze._
import breeze.linalg._
import breeze.linalg.{DenseMatrix => BreezeDenseMatrix, DenseVector}
import org.apache.flink.api.common.state.AggregatingState

case class ORR() extends Learner {

  private val lambda: Double = 0.0
  private val epsilon: Double = 0.1

  override def initialize_model(data: Point)(implicit gModel: AggregatingState[l_params, l_params]): Unit = {
    gModel add mlin_params(lambda * diag(DenseVector.fill(data.vector.size + 1) {
      0.0
    }),
      DenseVector.fill(data.vector.size + 1) {
        0.0
      })
  }

  override def predict(data: Point)(implicit mdl: AggregatingState[l_params, l_params]): Option[Double] = {
    try {
      val parameters = mdl.get.asInstanceOf[mlin_params]
      val x: DenseVector[Double] = DenseVector.vertcat(
        data.vector.asBreeze.asInstanceOf[DenseVector[Double]],
        DenseVector.ones(1))
      //      println(pinv(parameters.A) * x)
      Some(parameters.b.t * pinv(parameters.A) * x)
    } catch {
      case e: Exception => e.printStackTrace()
        None
    }
  }

  override def fit(data: Point)(implicit mdl: AggregatingState[l_params, l_params]): Unit = {
    val x: DenseVector[Double] = DenseVector.vertcat(
      data.vector.asBreeze.asInstanceOf[DenseVector[Double]],
      DenseVector.ones(1))
    val a: DenseMatrix[Double] = x * x.t
    mdl add mlin_params(a, data.asInstanceOf[LabeledPoint].label * x)
  }

  override def score(test_set: Array[Point])(implicit mdl: AggregatingState[l_params, l_params]): Option[Double] = {
    try {
      if (test_set.length > 0) {
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

  override def toString: String = s"ORR ${this.hashCode}"

}

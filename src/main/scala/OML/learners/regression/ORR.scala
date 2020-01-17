package OML.learners.regression

import OML.learners.{Learner, OnlineLearner}
import OML.parameters.{LearningParameters => l_params, MatrixLinearModelParameters => mlin_params}
import OML.math.Breeze._
import breeze.linalg.{DenseVector => BreezeDenseVector}
import OML.math.{LabeledPoint, Point}
import OML.utils.parsers.StringToArrayDoublesParser
import breeze.linalg._
import breeze.linalg.DenseVector
import org.apache.flink.api.common.state.AggregatingState

import scala.collection.mutable
import scala.collection.mutable.ListBuffer

case class ORR() extends OnlineLearner {

  private var lambda: Double = 0.0

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
    weights = model_init(data.vector.size + 1)
  }

  override def initialize_model_safe(data: Point)(implicit gModel: AggregatingState[l_params, l_params]): Unit = {
    gModel add model_init(data.vector.size + 1)
  }

  override def predict(data: Point): Option[Double] = {
    try {
      val x: DenseVector[Double] = add_bias(data)
      Some(weights.asInstanceOf[mlin_params].b.t * pinv(weights.asInstanceOf[mlin_params].A) * x)
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
      weights += mlin_params(a, data.asInstanceOf[LabeledPoint].label * x)
    } catch {
      case _: Exception =>
        if (weights == null) initialize_model(data)
        fit(data)
    }
  }

  override def fit_safe(data: Point)(implicit mdl: AggregatingState[l_params, l_params]): Unit = {
    val x: DenseVector[Double] = add_bias(data)
    val a: DenseMatrix[Double] = x * x.t
    mdl add mlin_params(a, data.asInstanceOf[LabeledPoint].label * x)
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
          (for (_ <- 0 until test_set_size)
            yield {
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

  def setLambda(lambda: Double): ORR = {
    this.lambda = lambda
    this
  }

  override def setParameters(parameterMap: mutable.Map[String, Any]): Learner = {
    for ((parameter, value) <- parameterMap) {
      parameter match {
        case "A" =>
          try {
            weights.asInstanceOf[mlin_params].A = BreezeDenseVector[Double](StringToArrayDoublesParser
              .parse(value.asInstanceOf[String])).toDenseMatrix
          } catch {
            case e: Exception =>
              println("Error while trying to update the matrix A of ORR regressor")
              e.printStackTrace()
          }
        case "b" =>
          try {
            weights.asInstanceOf[mlin_params].b = BreezeDenseVector[Double](StringToArrayDoublesParser
              .parse(value.asInstanceOf[String]))
          } catch {
            case e: Exception =>
              println("Error while trying to update the intercept flag of ORR regressor")
              e.printStackTrace()
          }
        case _ =>
      }
    }
    this
  }

  override def setHyperParameters(hyperParameterMap: mutable.Map[String, Any]): Learner = {
    for ((hyperparameter, value) <- hyperParameterMap) {
      hyperparameter match {
        case "lambda" =>
          try {
            setLambda(value.asInstanceOf[Double])
          } catch {
            case e: Exception =>
              println("Error while trying to update the epsilon hyperparameter of PA regressor")
              e.printStackTrace()
          }
        case _ =>
      }
    }
    this
  }

  override def toString: String = s"ORR ${this.hashCode}"

}

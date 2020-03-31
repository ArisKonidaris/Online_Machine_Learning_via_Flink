package oml.mlAPI.learners.regression

import oml.FlinkBipartiteAPI.POJOs
import oml.mlAPI.math.Breeze._
import oml.mlAPI.math.{LabeledPoint, Point, Vector}
import oml.mlAPI.learners.{Learner, OnlineLearner}
import oml.mlAPI.parameters.{Bucket, LearningParameters, ParameterDescriptor, MatrixModelParameters => matrix_params}

import scala.collection.mutable
import scala.collection.mutable.ListBuffer
import scala.collection.JavaConverters._
import breeze.linalg.{DenseVector => BreezeDenseVector, _}

/**
  * Online Ridge Regression Learner.
  */
case class ORR() extends OnlineLearner {

  protected var weights: matrix_params = _

  override def generateParameters: ParameterDescriptor => LearningParameters = new matrix_params().generateParameters

  override def getSerializedParams: (LearningParameters , Boolean, Bucket) => (Array[Int], Vector) =
    new matrix_params().generateSerializedParams

  private var lambda: Double = 0.0

  private def model_init(n: Int): matrix_params = {
    matrix_params(lambda * diag(BreezeDenseVector.fill(n) {0.0}),
      BreezeDenseVector.fill(n) {0.0}
    )
  }

  private def add_bias(data: Point): BreezeDenseVector[Double] = {
    BreezeDenseVector.vertcat(
      data.vector.asBreeze.asInstanceOf[BreezeDenseVector[Double]],
      BreezeDenseVector.ones(1))
  }

  override def initialize_model(data: Point): Unit = {
    weights = model_init(data.vector.size + 1)
  }

  override def predict(data: Point): Option[Double] = {
    try {
      val x: BreezeDenseVector[Double] = add_bias(data)
      Some(weights.b.t * pinv(weights.A) * x)
    } catch {
      case e: Exception => e.printStackTrace()
        None
    }
  }

  override def fit(data: Point): Unit = {
    val x: BreezeDenseVector[Double] = add_bias(data)
    val a: DenseMatrix[Double] = x * x.t
    try {
      weights += matrix_params(a, data.asInstanceOf[LabeledPoint].label * x)
    } catch {
      case _: Exception =>
        if (weights == null) initialize_model(data)
        fit(data)
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

  def setLambda(lambda: Double): ORR = {
    this.lambda = lambda
    this
  }

  override def setParametersFromMap(parameterMap: mutable.Map[String, AnyRef]): Learner = {
    for ((parameter, value) <- parameterMap) {
      parameter match {
        case "A" =>
          try {
            val new_weights = BreezeDenseVector[Double](
              value.asInstanceOf[java.util.List[Double]].asScala.toArray
            ).toDenseMatrix
            if (weights == null || weights.A.size == new_weights.size)
              weights.A = new_weights
            else
              throw new RuntimeException("Invalid size of new A matrix for the ORR regressor")
          } catch {
            case e: Exception =>
              println("Error while trying to update the matrix A of ORR regressor")
              e.printStackTrace()
          }
        case "b" =>
          try {
            val new_bias = BreezeDenseVector[Double](value.asInstanceOf[java.util.List[Double]].asScala.toArray)
            if (weights == null || weights.b.size == new_bias.size)
              weights.b = new_bias
            else
              throw new RuntimeException("Invalid size of new b vector for the ORR regressor")
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

  override def setHyperParametersFromMap(hyperParameterMap: mutable.Map[String, AnyRef]): Learner = {
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

  override def generatePOJOLearner: POJOs.Learner = {
    new POJOs.Learner("ORR",
      Map[String, AnyRef](("lambda", lambda.asInstanceOf[AnyRef])).asJava,
      Map[String, AnyRef](
        ("a", if(weights == null) null else weights.A.data.asInstanceOf[AnyRef]),
        ("b", if(weights == null) null else weights.b.data.asInstanceOf[AnyRef])
      ).asJava
    )
  }

  override def getParameters: Option[LearningParameters] = Some(weights)

  override def setParameters(params: LearningParameters): Learner = {
    assert(params.isInstanceOf[matrix_params])
    weights = params.asInstanceOf[matrix_params]
    this
  }
}

package oml.mlAPI.learners

import oml.mlAPI.math.Breeze._
import oml.mlAPI.math.{Point, Vector}
import oml.mlAPI.parameters.{Bucket, LearningParameters, ParameterDescriptor, VectorBias}
import breeze.linalg.{DenseVector => BreezeDenseVector}

import scala.collection.JavaConverters._
import scala.collection.mutable

abstract class PassiveAggressiveLearners extends OnlineLearner {

  protected var updateType: String = "PA-II"

  protected var C: Double = 0.01

  protected var weights: VectorBias = _

  override def initialize_model(data: Point): Unit = {
    weights = VectorBias(BreezeDenseVector.zeros[Double](data.getVector.size), 0.0)
  }

  protected def predictWithMargin(data: Point): Option[Double] = {
    try {
      Some((data.vector.asBreeze dot weights.weights) + weights.intercept)
    } catch {
      case _: Throwable => None
    }
  }

  protected def LagrangeMultiplier(loss: Double, data: Point): Double = {
    updateType match {
      case "STANDARD" => loss / (1.0 + ((data.vector dot data.vector) + 1.0))
      case "PA-I" => Math.min(C, loss / ((data.vector dot data.vector) + 1.0))
      case "PA-II" => loss / (((data.vector dot data.vector) + 1.0) + 1.0 / (2.0 * C))
    }
  }

  protected def checkParameters(data: Point): Unit = {
    if (weights == null) {
      initialize_model(data)
    } else {
      if (weights.weights.size != data.getVector.size)
        throw new RuntimeException("Incompatible model and data point size.")
      else
        throw new RuntimeException("Something went wrong while fitting the data point " +
          data + " to learner " + this + ".")
    }
  }

  override def getParameters: Option[LearningParameters] = Option(weights)

  override def setParameters(params: LearningParameters): Learner = {
    assert(params.isInstanceOf[VectorBias])
    weights = params.asInstanceOf[VectorBias]
    this
  }

  def setC(c: Double): PassiveAggressiveLearners = {
    this.C = c
    this
  }

  def setType(updateType: String): PassiveAggressiveLearners = {
    this.updateType = updateType
    this
  }

  override def setParametersFromMap(parameterMap: mutable.Map[String, AnyRef]): Learner = {
    for ((parameter, value) <- parameterMap) {
      parameter match {
        case "a" =>
          try {
            val new_weights = BreezeDenseVector[Double](value.asInstanceOf[java.util.List[Double]].asScala.toArray)
            if (weights == null || weights.weights.size == new_weights.size)
              weights.weights = new_weights
            else
              throw new RuntimeException("Invalid size of new weight vector for the PA classifier.")
          } catch {
            case e: Exception =>
              println("Error while trying to update the weights of the PA classifier.")
              e.printStackTrace()
          }
        case "b" =>
          try {
            weights.intercept = value.asInstanceOf[Double]
          } catch {
            case e: Exception =>
              println("Error while trying to update the intercept of the PA classifier.")
              e.printStackTrace()
          }
        case _ =>
      }
    }
    this
  }

  override def generateParameters: ParameterDescriptor => LearningParameters = new VectorBias().generateParameters

  override def getSerializedParams: (LearningParameters , Boolean, Bucket) => (Array[Int], Vector) =
    new VectorBias().generateSerializedParams

}

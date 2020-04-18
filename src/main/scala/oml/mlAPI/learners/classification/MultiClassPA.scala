package oml.mlAPI.learners.classification

import oml.FlinkBipartiteAPI.POJOs
import oml.mlAPI.math.Breeze._
import oml.mlAPI.math.{LabeledPoint, Point, Vector}
import oml.mlAPI.learners.{Learner, OnlineLearner}
import oml.mlAPI.parameters.{Bucket, LearningParameters, ParameterDescriptor, VectorBias, VectorBiasList}
import breeze.linalg.{DenseVector => BreezeDenseVector}
import oml.mlAPI.scores.{Accuracy, Scores}

import scala.collection.mutable
import scala.collection.mutable.ListBuffer
import scala.collection.JavaConverters._

/**
  * Multi-class Passive Aggressive Classifier.
  */
case class MultiClassPA() extends OnlineLearner with Classifier with Serializable {

  protected var updateType: String = "PA-II"

  protected var C: Double = 0.01

  protected var weights: VectorBiasList = _

  protected var nClasses: Int = 3

  override def initialize_model(data: Point): Unit = {
    val vbl: ListBuffer[VectorBias] = ListBuffer[VectorBias]
    for (_ <- 0 until nClasses) vbl.append(VectorBias(BreezeDenseVector.zeros[Double](data.getVector.size), 0.0))
    weights = VectorBiasList(vbl)
  }

  override def predict(data: Point): Option[Double] = {
    try {
      var prediction: Int = -1
      var highestScore: Double = -Double.MaxValue
      for ((model: VectorBias, i: Int) <- weights.vectorBiases.zipWithIndex) {
        val currentClassScore = (data.vector.asBreeze dot model.weights) + model.intercept
        if (currentClassScore > highestScore) {
          prediction = i
          highestScore = currentClassScore
        }
      }
      Some(1.0 * prediction)
    } catch {
      case _: Throwable => None
    }
  }

  override def fit(data: Point): Unit = {
    fitLoss(data)
    ()
  }

  override def fitLoss(data: Point): Double = {
    predict(data) match {
      case Some(prediction) =>
        val label: Double = data.asInstanceOf[LabeledPoint].label
        val pred: Int = prediction.toInt
        val loss: Double = {
          val exp_weights = weights.vectorBiases(label.toInt)
          val pred_weights = weights.vectorBiases(pred)
          1.0 - (
            ((data.vector.asBreeze dot exp_weights.weights) + exp_weights.intercept)
              -
              ((data.vector.asBreeze dot pred_weights.weights) + pred_weights.intercept)
            )
        }
        for ((weight: VectorBias, i: Int) <- weights.vectorBiases.zipWithIndex)
          if (i != label && i != pred)
            ()
          else {
            val t: Double = tau(loss, data)
            if (i == label)
              weight += VectorBias((data.vector.asBreeze * t).asInstanceOf[BreezeDenseVector[Double]], t)
            else if (i == pred)
              weight -= VectorBias((data.vector.asBreeze * t).asInstanceOf[BreezeDenseVector[Double]], t)
          }
        loss
      case None =>
        checkParameters(data)
        fitLoss(data)
    }
  }

  override def score(test_set: ListBuffer[Point]): Double =
    Scores.accuracy(test_set.asInstanceOf[ListBuffer[LabeledPoint]], this)

  private def tau(loss: Double, data: Point): Double = {
    updateType match {
      case "STANDARD" => loss / (1.0 + 2.0 * ((data.vector dot data.vector) + 1.0))
      case "PA-I" => Math.min(C / 2.0, loss / (2.0 * ((data.vector dot data.vector) + 1.0)))
      case "PA-II" => 0.5 * (loss /(((data.vector dot data.vector) + 1.0) + 1.0 / (2.0 * C)))
    }
  }

  private def setNumberOfClasses(nClasses: Int): Unit = this.nClasses = nClasses

  private def checkParameters(data: Point): Unit = {
    if (weights == null) {
      initialize_model(data)
    } else {
      if(weights.vectorBiases.head.weights.length != data.getVector.size)
        throw new RuntimeException("Incompatible model and data point size.")
      else
        throw new RuntimeException("Something went wrong while fitting the data point " +
          data + " to learner " + this + ".")
    }
  }

  override def getParameters: Option[LearningParameters] = Option(weights)

  override def setParameters(params: LearningParameters): Learner = {
    assert(params.isInstanceOf[VectorBiasList])
    weights = params.asInstanceOf[VectorBiasList]
    this
  }

  def setC(c: Double): MultiClassPA = {
    this.C = c
    this
  }

  def setType(updateType: String): MultiClassPA = {
    this.updateType = updateType
    this
  }

  override def setParametersFromMap(parameterMap: mutable.Map[String, AnyRef]): Learner = {
    for ((parameter, value) <- parameterMap) {
      parameter match {
        case "weights" =>
          try {
            val vbl: ListBuffer[VectorBias] = ListBuffer[VectorBias]
            for (v: java.util.List[Double] <- value.asInstanceOf[java.util.List[java.util.List[Double]]].asScala)
              vbl.append(new VectorBias(v.asScala.toArray))
            val new_weights = VectorBiasList(vbl)
            if (weights == null || weights.size == new_weights.size)
              weights = new_weights
            else
              throw new RuntimeException("Invalid size of new weight vector for the multiclass PA classifier.")
          } catch {
            case e: Exception =>
              println("Error while trying to update the weights of the multiclass PA classifier.")
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
        case "C" =>
          try {
            setC(value.asInstanceOf[Double])
          } catch {
            case e: Exception =>
              println("Error while trying to update the C hyper parameter of the multiclass PA classifier.")
              e.printStackTrace()
          }
        case "updateType" =>
          try {
            setType(value.asInstanceOf[String])
          } catch {
            case e: Exception =>
              println("Error while trying to update the update type of the multiclass PA classifier.")
              e.printStackTrace()
          }
        case "nClasses" =>
          try {
            setNumberOfClasses(value.asInstanceOf[Double].toInt)
          } catch {
            case e: Exception =>
              println("Error while trying to update the number of classes " +
                "hyper parameter of the multiclass PA classifier.")
              e.printStackTrace()
          }
        case _ =>
      }
    }
    this
  }

  override def toString: String = s"MulticlassPA classifier ${this.hashCode}"

  override def generateParameters: ParameterDescriptor => LearningParameters = new VectorBiasList().generateParameters

  override def getSerializedParams: (LearningParameters , Boolean, Bucket) => (Array[Int], Vector) =
    new VectorBiasList().generateSerializedParams

  override def generatePOJOLearner: POJOs.Learner = {
    new POJOs.Learner("MulticlassPA",
      Map[String, AnyRef](("C", C.asInstanceOf[AnyRef])).asJava,
      Map[String, AnyRef](
        ("weights",
          if(weights == null)
            null
          else
            (for (weight <- weights.vectorBiases) yield weight.flatten.data).toArray.asInstanceOf[AnyRef]
        )
      ).asJava
    )
  }

}

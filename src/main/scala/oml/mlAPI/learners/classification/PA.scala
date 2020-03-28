package oml.mlAPI.learners.classification

import oml.FlinkBipartiteAPI.POJOs
import oml.mlAPI.math.Breeze._
import oml.mlAPI.math.{LabeledPoint, Point}
import oml.mlAPI.learners.{Learner, PassiveAggressiveLearners}
import oml.mlAPI.parameters.{LinearModelParameters => linear_params}

import breeze.linalg.{DenseVector => BreezeDenseVector}
import scala.collection.mutable
import scala.collection.mutable.ListBuffer
import scala.collection.JavaConverters._

/**
  * Passive Aggressive Classifier
  */
case class PA() extends PassiveAggressiveLearners {

  override def fit(data: Point): Unit = {
    predictWithMargin(data) match {
      case Some(prediction) =>
        val label: Double = zeroLabel(data.asInstanceOf[LabeledPoint].label)
        if (checkLabel(label)) {
          val loss: Double = 1.0 - label * prediction
          if (loss > 0.0) {
            val Lagrange_Multiplier: Double = LagrangeMultiplier(loss, data)
            weights += linear_params(
              (data.vector.asBreeze * (Lagrange_Multiplier * label)).asInstanceOf[BreezeDenseVector[Double]],
              Lagrange_Multiplier * label)
          }
        }
      case None =>
        if (weights == null) initialize_model(data)
        fit(data)
    }
  }

  override def score(test_set: ListBuffer[Point]): Option[Double] = {
    try {
      if (test_set.nonEmpty && weights != null) {
        Some((for (test <- test_set) yield {
          val prediction: Double = predict(test).get
          if (test.asInstanceOf[LabeledPoint].label == prediction) 1 else 0
        }).sum / (1.0 * test_set.length))
      } else None
    } catch {
      case _: Throwable => None
    }
  }

  private def zeroLabel(label: Double): Double = if (label == 0.0) -1.0 else label

  private def checkLabel(label: Double): Boolean = label == 1.0 || label == -1.0

  override def setHyperParametersFromMap(hyperParameterMap: mutable.Map[String, AnyRef]): Learner = {
    for ((hyperparameter, value) <- hyperParameterMap) {
      hyperparameter match {
        case "C" =>
          try {
            setC(value.asInstanceOf[Double])
          } catch {
            case e: Exception =>
              println("Error while trying to update the C hyper parameter of PA classifier")
              e.printStackTrace()
          }
        case _ =>
      }
    }
    this
  }

  override def toString: String = s"PA classifier ${this.hashCode}"

  override def generatePOJOLearner: POJOs.Learner = {
    new POJOs.Learner("PA",
      Map[String, AnyRef](("C", C.asInstanceOf[AnyRef])).asJava,
      Map[String, AnyRef](
        ("a", if(weights == null) null else weights.weights.data.asInstanceOf[AnyRef]),
        ("b", if(weights == null) null else weights.intercept.asInstanceOf[AnyRef])
      ).asJava
    )
  }

}

package oml.mlAPI.learners.classification

import breeze.linalg.{DenseVector => BreezeDenseVector}
import oml.math.Breeze._
import oml.math.{LabeledPoint, Point}
import oml.mlAPI.learners.{Learner, PassiveAggressiveLearners}
import oml.parameters.{LinearModelParameters => lin_params}

import scala.collection.mutable
import scala.collection.mutable.ListBuffer

/** Implementation of Passive Aggressive Classifier */
case class PA() extends PassiveAggressiveLearners {

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

  private def zeroLabel(label: Double): Double = if (label == 0.0) -1.0 else label

  private def checkLabel(label: Double): Boolean = label == 1.0 || label == -1.0

  override def setHyperParameters(hyperParameterMap: mutable.Map[String, Any]): Learner = {
    for ((hyperparameter, value) <- hyperParameterMap) {
      hyperparameter match {
        case "C" =>
          try {
            setC(value.asInstanceOf[Double])
          } catch {
            case e: Exception =>
              println("Error while trying to update the C hyperparameter of PA classifier")
              e.printStackTrace()
          }
        case _ =>
      }
    }
    this
  }

  override def toString: String = s"PA classifier ${this.hashCode}"

}

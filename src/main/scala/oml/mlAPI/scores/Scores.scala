package oml.mlAPI.scores

import oml.mlAPI.learners.classification.Classifier
import oml.mlAPI.learners.clustering.Clusterer
import oml.mlAPI.learners.regression.Regressor
import oml.mlAPI.math.{LabeledPoint, Point}

import scala.collection.mutable.ListBuffer

object Scores {

  def accuracy(testSet: ListBuffer[LabeledPoint], learner: Classifier): Double = {
    try {
      if (testSet.nonEmpty) {
        (for (test <- testSet) yield {
          val prediction: Double = learner.predict(test).get
          if (test.asInstanceOf[LabeledPoint].label == prediction) 1 else 0
        }).sum / (1.0 * testSet.length)
      } else 0.0
    } catch {
      case _: Throwable => 0.0
    }
  }

  def RMSE(testSet: ListBuffer[LabeledPoint], learner: Regressor): Double = {
    try {
      if (testSet.nonEmpty) {
        Math.sqrt(
          (for (test <- testSet) yield {
            learner.predict(test) match {
              case Some(pred) => Math.pow(test.asInstanceOf[LabeledPoint].label - pred, 2)
              case None => Double.MaxValue
            }
          }).sum / (1.0 * testSet.length)
        )
      } else Double.MaxValue
    } catch {
      case _: Throwable => Double.MaxValue
    }
  }

  def F1Score(testSet: ListBuffer[LabeledPoint], learner: Classifier): Double = {
    var truePositive: Int = 0
    var falsePositive: Int = 0
    var trueNegative: Int = 0
    var FalseNegative: Int = 0
    try {
      if (testSet.nonEmpty) {
        for (point: LabeledPoint <- testSet) {
          val prediction: Double = learner.predict(point).get
          if (point.label > 0.0 && prediction > 0.0)
            truePositive += 1
          else if (point.label > 0.0 && prediction < 0.0)
            FalseNegative += 1
          else if (point.label < 0.0 && prediction > 0.0)
            falsePositive += 1
          else if (point.label < 0.0 && prediction < 0.0)
            trueNegative += 1
        }
        val precision: Double = (1.0 * truePositive) / (truePositive + falsePositive)
        val recall: Double = (1.0 * truePositive) / (truePositive + FalseNegative)
        2.0 * (precision * recall) / (precision + recall)
      } else 0.0
    } catch {
      case _: Throwable => 0.0
    }
  }

  def inertia(testSet: ListBuffer[Point], learner: Clusterer): Double = {
    if (testSet.nonEmpty) {
      @scala.annotation.tailrec
      def accumulateLoss(index: Int, loss: Double): Double = {
        require(index >= 0 && index <= testSet.length - 1)
        val dist: Array[Double] = learner.distribution(testSet(index))
        if (!dist.isEmpty) {
          val testLoss: Double = Math.pow(dist.min, 2)
          if (index == testSet.length - 1)
            loss + testLoss
           else
            accumulateLoss(index + 1, loss + testLoss)
        } else Double.MaxValue
      }
      accumulateLoss(0, 0.0)
    } else Double.MaxValue
  }

}

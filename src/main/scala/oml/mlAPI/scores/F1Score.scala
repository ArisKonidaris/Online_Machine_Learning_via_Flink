package oml.mlAPI.scores

import oml.mlAPI.learners.classification.Classifier
import oml.mlAPI.math.LabeledPoint

import scala.collection.mutable.ListBuffer

case class F1Score(TruePositive: Int,
                   FalsePositive: Int,
                   TrueNegative: Int,
                   FalseNegative: Int,
                   accuracy: Double,
                   f1: Double)
  extends Score {

  override def getScore: Double = f1

  override def toString: String = {
    "#####################################\n" +
      "True Positive  : " + TruePositive + "\n" +
      "False Positive : " + FalsePositive + "\n" +
      "True Negative  : " + TrueNegative + "\n" +
      "False Negative : " + FalseNegative + "\n" +
      "Accuracy       : " + accuracy + "\n" +
      "F1-Score       : " + f1 + "\n#####################################"
  }

}

object F1Score {
  def calculateScore(testSet: ListBuffer[LabeledPoint], learner: Classifier): F1Score = {
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
        val accuracy: Double =
          (1.0 * (truePositive + trueNegative)) / (truePositive + falsePositive + FalseNegative + trueNegative)
        val f1 = 2.0 * (precision * recall) / (precision + recall)
        F1Score(truePositive, falsePositive, trueNegative, FalseNegative, accuracy, f1)
      } else F1Score(0, 0, 0, 0, 0.0, 0.0)
    } catch {
      case _: Throwable => F1Score(0, 0, 0, 0, 0.0, 0.0)
    }
  }
}

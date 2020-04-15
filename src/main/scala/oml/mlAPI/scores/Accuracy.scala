package oml.mlAPI.scores

import oml.mlAPI.learners.classification.Classifier
import oml.mlAPI.math.LabeledPoint

import scala.collection.mutable.ListBuffer

case class Accuracy(accuracy: Double) extends Score {

  override def getScore: Double = accuracy

  override def toString: String = "Accuracy: " + accuracy

}

object Accuracy {
  def calculateScore(testSet: ListBuffer[LabeledPoint], learner: Classifier): Accuracy = {
    try {
      if (testSet.nonEmpty) {
        Accuracy((for (test <- testSet) yield {
          val prediction: Double = learner.predict(test).get
          if (test.asInstanceOf[LabeledPoint].label == prediction) 1 else 0
        }).sum / (1.0 * testSet.length))
      } else Accuracy(0.0)
    } catch {
      case _: Throwable => Accuracy(0.0)
    }
  }
}

package oml.mlAPI.scores

import oml.mlAPI.learners.regression.Regressor
import oml.mlAPI.math.LabeledPoint

import scala.collection.mutable.ListBuffer

case class RMSE(rmse: Double) extends Score {

  override def getScore: Double = rmse

  override def toString: String = {
    "#####################################\n" +
      "rmse: " + rmse + "\n#####################################"
  }

}

object RMSE {
  def calculateScore(testSet: ListBuffer[LabeledPoint], learner: Regressor): RMSE = {
    try {
      if (testSet.nonEmpty) {
        RMSE(
          Math.sqrt(
            (for (test <- testSet) yield {
              learner.predict(test) match {
                case Some(pred) => Math.pow(test.asInstanceOf[LabeledPoint].label - pred, 2)
                case None => Double.MaxValue
              }
            }).sum / (1.0 * testSet.length)
          )
        )
      } else RMSE(Double.MaxValue)
    } catch {
      case _: Throwable => RMSE(Double.MaxValue)
    }
  }
}

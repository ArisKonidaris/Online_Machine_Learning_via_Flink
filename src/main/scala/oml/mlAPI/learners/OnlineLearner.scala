package oml.mlAPI.learners

import oml.mlAPI.math.Point

import scala.collection.mutable.ListBuffer

abstract class OnlineLearner extends Learner {

  override def fit(batch: ListBuffer[Point]): Unit = {
    fitLoss(batch)
    ()
  }

  override def fitLoss(batch: ListBuffer[Point]): Double = (for (point <- batch) yield fitLoss(point)).sum
}


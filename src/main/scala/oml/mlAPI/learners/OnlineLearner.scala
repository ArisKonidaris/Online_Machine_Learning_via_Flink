package oml.mlAPI.learners

import oml.math.Point

import scala.collection.mutable.ListBuffer

abstract class OnlineLearner extends Learner {
  override def fit(batch: ListBuffer[Point]): Unit = for (point <- batch) fit(point)
}

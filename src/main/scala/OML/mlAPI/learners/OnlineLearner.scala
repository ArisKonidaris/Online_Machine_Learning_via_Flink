package OML.mlAPI.learners

import OML.math.Point
import scala.collection.mutable.ListBuffer

abstract class OnlineLearner extends Learner {
  override def fit(batch: ListBuffer[Point]): Unit = for (point <- batch) fit(point)
}

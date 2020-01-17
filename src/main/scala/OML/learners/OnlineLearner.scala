package OML.learners

import OML.math.Point
import org.apache.flink.api.common.state.AggregatingState
import OML.parameters.{LearningParameters => l_params}

import scala.collection.mutable.ListBuffer

abstract class OnlineLearner extends Learner {
  override def fit(batch: ListBuffer[Point]): Unit = for (point <- batch) fit(point)

  override def fit_safe(batch: ListBuffer[Point])(implicit mdl: AggregatingState[l_params, l_params]): Unit = {
    for (point <- batch) fit_safe(point)
  }
}

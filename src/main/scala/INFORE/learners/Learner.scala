package INFORE.learners

import INFORE.common.Point
import INFORE.parameters.{LearningParameters => l_params}
import org.apache.flink.api.common.state.AggregatingState

/** Contains the necessary methods needed by the worker/slave node
  * to train on it's local incoming data stream
  */
trait Learner extends Serializable {
  def initialize_model(data: Point)(implicit gModel: AggregatingState[l_params, l_params]): Unit

  def predict(data: Point)(implicit mdl: AggregatingState[l_params, l_params]): Option[Double]

  def fit(data: Point)(implicit mdl: AggregatingState[l_params, l_params]): Unit

  def score(test_set: Array[Point])(implicit mdl: AggregatingState[l_params, l_params]): Option[Double]
}

object Learner

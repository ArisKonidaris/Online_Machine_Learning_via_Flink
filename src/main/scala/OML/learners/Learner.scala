package OML.learners

import OML.common.Point
import OML.parameters.{LearningParameters => l_params}
import org.apache.flink.api.common.state.AggregatingState

import scala.collection.mutable.ListBuffer

/** Contains the necessary methods needed by the worker/slave node
  * to train on it's local incoming data stream
  */
trait Learner extends Serializable {

  protected var parameters: l_params = _

  def get_params(): l_params = parameters

  def set_params(params: l_params): Unit = parameters = params

  def initialize_model(data: Point): Unit

  def initialize_model_safe(data: Point)(implicit gModel: AggregatingState[l_params, l_params]): Unit

  def predict(data: Point): Option[Double]

  def predict_safe(data: Point)(implicit mdl: AggregatingState[l_params, l_params]): Option[Double]

  def fit(data: Point): Unit

  def fit_safe(data: Point)(implicit mdl: AggregatingState[l_params, l_params]): Unit

  def score(test_set: ListBuffer[Point]): Option[Double]

  def score_safe(test_set: ListBuffer[Point])(implicit mdl: AggregatingState[l_params, l_params]): Option[Double]
}

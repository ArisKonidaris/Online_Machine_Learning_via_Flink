package OML.learners

import OML.common.{Parameter, ParameterMap, WithParameters}
import OML.math.Point
import OML.parameters.{LearningParameters => l_params}
import org.apache.flink.api.common.state.AggregatingState

import scala.collection.mutable
import scala.collection.mutable.ListBuffer

/** Contains the necessary methods needed by the worker/slave node
  * to train on it's local incoming data stream
  */
trait Learner extends Serializable {

  protected var weights: l_params = _

  protected var update_complexity: Int = _


  // =================================== Main methods ==============================================


  def getParameters: Option[l_params] = Option(weights)

  def setParameters(params: l_params): Learner = {
    weights = params
    this
  }

  def setParameters(parameterMap: mutable.Map[String, Any]): Learner

  def setHyperParameters(hyperParameterMap: mutable.Map[String, Any]): Learner

  def initialize_model(data: Point): Unit

  def initialize_model_safe(data: Point)(implicit gModel: AggregatingState[l_params, l_params]): Unit

  def predict(data: Point): Option[Double]

  def predict_safe(data: Point)(implicit mdl: AggregatingState[l_params, l_params]): Option[Double]

  def fit(data: Point): Unit

  def fit(batch: ListBuffer[Point]): Unit

  def fit_safe(data: Point)(implicit mdl: AggregatingState[l_params, l_params]): Unit

  def fit_safe(batch: ListBuffer[Point])(implicit mdl: AggregatingState[l_params, l_params]): Unit

  def score(test_set: ListBuffer[Point]): Option[Double]

  def score_safe(test_set: AggregatingState[Point, Option[Point]], test_set_size: Int)
                (implicit mdl: AggregatingState[l_params, l_params]): Option[Double]

}

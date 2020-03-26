package oml.mlAPI.learners

import oml.math.Point
import oml.mlAPI.WithParams
import oml.parameters.{Bucket, ParameterDescriptor, LearningParameters => l_params}

import scala.collection.mutable
import scala.collection.mutable.ListBuffer

/** Contains the necessary methods needed by the workers/slave node
  * to train on it's local incoming data stream.
  */
trait Learner extends Serializable with WithParams {

  protected var weights: l_params = _

  protected var update_complexity: Int = _


  // =================================== Main methods ==============================================


  def getParameters: Option[l_params] = Option(weights)

  def setParameters(params: l_params): Learner = {
    weights = params
    this
  }

  override def setHyperParameters(hyperParameterMap: mutable.Map[String, AnyRef]): Learner = this

  override def addHyperParameter(key: String, value: AnyRef): Learner = this

  override def removeHyperParameter(key: String, value: AnyRef): Learner = this

  override def setParameters(parameterMap: mutable.Map[String, AnyRef]): Learner = this

  override def addParameter(key: String, value: AnyRef): Learner = this

  override def removeParameter(key: String, value: AnyRef): Learner = this

  def initialize_model(data: Point): Unit

  def predict(data: Point): Option[Double]

  def fit(data: Point): Unit

  def fit(batch: ListBuffer[Point]): Unit

  def score(test_set: ListBuffer[Point]): Option[Double]

  def generateParameters: ParameterDescriptor => l_params

  def generateDescriptor: (l_params , Boolean, Bucket) => ParameterDescriptor

}

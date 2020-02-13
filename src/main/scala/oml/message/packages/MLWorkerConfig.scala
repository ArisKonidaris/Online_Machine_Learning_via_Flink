package oml.message.packages

import oml.mlAPI.types.protocols.{ProtocolType, AsynchronousAveraging => Async}

import scala.collection.mutable.{HashMap => HMap, Map => HashTable}

case class MLWorkerConfig(var parameters: HashTable[String, Any],
                          var preprocessors: Option[List[TransformerContainer]],
                          var learner: Option[TransformerContainer],
                          var proto: ProtocolType)
  extends Container {

  def this() = this(HMap[String, Any](), None, None, Async)

  def this(params: HashTable[String, Any]) = this(params, None, None, Async)

  def this(pp: Option[List[TransformerContainer]]) = this(HMap[String, Any](), pp, None, Async)

  def this(pr: ProtocolType) = this(HMap[String, Any](), None, None, pr)

  def this(params: HashTable[String, Any], l: Option[TransformerContainer]) = this(params, None, l, Async)

  def this(params: HashTable[String, Any], pr: ProtocolType) = this(params, None, None, pr)

  def this(pp: Option[List[TransformerContainer]], l: Option[TransformerContainer]) =
    this(HMap[String, Any](), pp, l, Async)

  def this(l: Option[TransformerContainer], pr: ProtocolType) = this(HMap[String, Any](), None, l, pr)

  def getParameters: HashTable[String, Any] = parameters

  def getPreprocessors: Option[List[TransformerContainer]] = preprocessors

  def getLearner: Option[TransformerContainer] = learner

  def getProto: ProtocolType = proto

  def setParams(parameters: HashTable[String, Any]): Unit = this.parameters = parameters

  def setPreprocessors(preprocessors: Option[List[TransformerContainer]]): Unit = this.preprocessors = preprocessors

  def setLearner(learner: Option[TransformerContainer]): Unit = this.learner = learner

  def setProto(proto: ProtocolType): Unit = this.proto = proto

  override def addParameter(key: String, value: Any): MLWorkerConfig = {
    parameters += (key -> value)
    this
  }

  override def equals(obj: Any): Boolean = {
    obj match {
      case MLWorkerConfig(params, pp, l, pr) =>
        parameters.equals(params) && preprocessors.equals(pp) && learner.equals(l) && proto.equals(pr)
      case _ => false
    }
  }

  override def toString: String = s"MLWorkerConfig($parameters, $preprocessors, $learner, $proto)"

}

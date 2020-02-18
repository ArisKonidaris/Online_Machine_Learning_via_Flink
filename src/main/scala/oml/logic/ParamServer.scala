package oml.logic

import oml.StarProtocolAPI.RemoteOp
import oml.parameters.LearningParameters

trait ParamServer {

  @RemoteOp(0)
  def pullModel(): Unit

  @RemoteOp(1)
  def pushModel(model: LearningParameters): Unit

}

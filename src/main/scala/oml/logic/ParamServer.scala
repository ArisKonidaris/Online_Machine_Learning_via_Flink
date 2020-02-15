package oml.logic

import oml.StarProtocolAPI.RemoteOp
import oml.parameters.LearningParameters

trait ParamServer {

  @RemoteOp(0)
  def getModel: LearningParameters

  @RemoteOp(1)
  def saveModel(model: LearningParameters): Unit

}

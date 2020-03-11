package oml.logic

import oml.StarProtocolAPI.{RemoteOp, Response}
import oml.parameters.LearningParameters

trait ParamServer {

  @RemoteOp(0)
  def pullModel(): Response[LearningParameters]

  @RemoteOp(1)
  def pushModel(model: LearningParameters): Unit
//  def pushModel(model: LearningParameters): Response[LearningParameters]

}

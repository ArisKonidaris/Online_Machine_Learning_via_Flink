package oml.logic

import java.util.function.Consumer

import oml.StarProtocolAPI.{RemoteOp, Response}
import oml.parameters.LearningParameters

trait ParamServer {

  @RemoteOp(0)
  //def pullModel(): Unit
  def pullModel(@Response model: Consumer[LearningParameters]): Unit

  @RemoteOp(1)
  def pushModel(model: LearningParameters): Unit

}

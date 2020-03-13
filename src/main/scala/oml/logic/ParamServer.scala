package oml.logic

import oml.StarTopologyAPI.annotations.RemoteOp
import oml.StarTopologyAPI.futures.Response
import oml.parameters.LearningParameters

trait ParamServer {

  @RemoteOp
  def pullModel(): Response[LearningParameters]

  @RemoteOp
  def pushModel(model: LearningParameters): Response[LearningParameters]

}

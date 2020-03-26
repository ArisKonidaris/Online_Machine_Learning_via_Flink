package oml.mlAPI

import oml.StarTopologyAPI.{RemoteOp, Response}
import oml.parameters.ParameterDescriptor

trait ParamServer {

  @RemoteOp(0)
  def pullModel(): Response[ParameterDescriptor]

  @RemoteOp(1)
  def pushModel(model: ParameterDescriptor): Unit

}

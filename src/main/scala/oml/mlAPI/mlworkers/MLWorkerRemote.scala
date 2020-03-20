package oml.mlAPI.mlworkers

import oml.StarTopologyAPI.annotations.{RemoteOp, RemoteProxy}
import oml.mlAPI.parameters.ParameterDescriptor

@RemoteProxy
trait MLWorkerRemote {
  @RemoteOp
  def receiveGlobalModel(model: ParameterDescriptor): Unit
}

package oml.mlAPI.mlworkers.interfaces

import oml.StarTopologyAPI.annotations.{RemoteOp, RemoteProxy}
import oml.mlAPI.parameters.ParameterDescriptor

@RemoteProxy
trait MLWorkerRemote {

  @RemoteOp
  def updateModel(model: ParameterDescriptor): Unit

}

package oml.mlAPI.mlworkers

import oml.StarTopologyAPI.{RemoteOp, RemoteProxy}
import oml.parameters.ParameterDescriptor

@RemoteProxy
trait MLWorkerRemote {

  @RemoteOp(1)
  def updateModel(model: ParameterDescriptor): Unit

}

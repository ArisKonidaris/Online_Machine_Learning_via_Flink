package oml.mlAPI.mlworkers

import oml.StarTopologyAPI.annotations.{RemoteOp, RemoteProxy}
import oml.math.Vector

@RemoteProxy
trait MLWorkerRemote {
  @RemoteOp
  def receiveGlobalModel(model: Vector): Unit
}

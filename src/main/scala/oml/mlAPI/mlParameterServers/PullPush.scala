package oml.mlAPI.mlParameterServers

import oml.StarTopologyAPI.annotations.RemoteOp
import oml.StarTopologyAPI.futures.Response
import oml.math.Vector

trait PullPush extends Serializable {

  @RemoteOp
  def pullModel: Response[Vector]

  @RemoteOp
  def pushModel(model: Vector): Response[Vector]

}

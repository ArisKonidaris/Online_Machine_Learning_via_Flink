package oml.mlAPI.mlParameterServers

import oml.StarTopologyAPI.annotations.RemoteOp
import oml.StarTopologyAPI.futures.Response
import oml.mlAPI.parameters.ParameterDescriptor

trait PullPush extends Serializable {

  @RemoteOp
  def pullModel: Response[ParameterDescriptor]

  @RemoteOp
  def pushModel(model: ParameterDescriptor): Response[ParameterDescriptor]

}

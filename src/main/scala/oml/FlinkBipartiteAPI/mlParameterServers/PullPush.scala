package oml.FlinkBipartiteAPI.mlParameterServers

import oml.StarTopologyAPI.annotations.{RemoteOp, RemoteProxy}
import oml.StarTopologyAPI.futures.Response
import oml.mlAPI.parameters.ParameterDescriptor

@RemoteProxy
trait PullPush extends Serializable {

  @RemoteOp
  def pullModel: Response[ParameterDescriptor]

  @RemoteOp
  def pushModel(modelDescriptor: ParameterDescriptor): Response[ParameterDescriptor]

}

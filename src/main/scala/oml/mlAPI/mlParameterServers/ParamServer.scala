package oml.mlAPI.mlParameterServers

import oml.StarTopologyAPI.annotations.RemoteOp
import oml.StarTopologyAPI.futures.Response
import oml.StarTopologyAPI.sites.NetworkGraph

trait ParamServer extends PullPush {

  @RemoteOp
  def getInfo: Response[NetworkGraph]

}

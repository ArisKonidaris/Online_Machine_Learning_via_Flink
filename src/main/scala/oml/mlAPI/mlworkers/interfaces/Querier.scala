package oml.mlAPI.mlworkers.interfaces

import oml.FlinkBipartiteAPI.POJOs.QueryResponse
import oml.StarTopologyAPI.annotations.RemoteOp

trait Querier {

  @RemoteOp
  def sendQueryResponse(qResponse: QueryResponse): Unit

}

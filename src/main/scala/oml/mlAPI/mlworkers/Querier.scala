package oml.mlAPI.mlworkers

import oml.POJOs.QueryResponse
import oml.StarTopologyAPI.RemoteOp

trait Querier extends Serializable {

  @RemoteOp(0)
  def sendQueryResponse(qResponse: QueryResponse): Unit

}

package oml.FlinkBipartiteAPI.messages

import java.io.Serializable

import oml.FlinkBipartiteAPI.POJOs.Request
import oml.StarTopologyAPI.operations.RemoteCallIdentifier
import oml.StarTopologyAPI.sites.NodeId

abstract class FlinkMessage extends Serializable {
  var networkId: Int
  var operation: RemoteCallIdentifier
  var source: NodeId
  var destination: NodeId
  var data: Serializable
  var request: Request
}

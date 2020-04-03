package oml.FlinkBipartiteAPI.messages

import java.awt.JobAttributes.DestinationType
import java.io.Serializable

import oml.FlinkBipartiteAPI.POJOs.Request
import oml.StarTopologyAPI.operations.RemoteCallIdentifier
import oml.StarTopologyAPI.sites.NodeId

case class HubMessage(var networkId: Int,
                      var operations: Array[RemoteCallIdentifier],
                      var source: NodeId,
                      var destinations: Array[NodeId],
                      var data: Serializable,
                      var request: Request) extends Serializable {

  def this() = this(
    -1,
    new Array[RemoteCallIdentifier](1),
    new NodeId(null, -1),
    new Array[NodeId](1),
    null,
    new Request()
  )

  def getNetworkId: Int = networkId

  def setNetworkId(networkId: Int): Unit = this.networkId = networkId

  def getOperations: Array[RemoteCallIdentifier] = operations

  def setOperations(operations: Array[RemoteCallIdentifier]): Unit = this.operations = operations

  def getSource: NodeId = source

  def setSource(source: NodeId): Unit = this.source = source

  def getDestinations: Array[NodeId] = destinations

  def setDestinations(destinations: Array[NodeId]): Unit = this.destinations = destinations

  def getData: Serializable = data

  def setData(data: Serializable): Unit = this.data = data

  def getRequest: Request = request

  def setRequest(request: Request): Unit = this.request = request

  override def equals(obj: Any): Boolean = {
    obj match {
      case HubMessage(net, ops, src, dsts, dt, req) =>
        networkId == net &&
          operations.equals(ops) &&
          source == src &&
          destinations .equals(dsts) &&
          data.equals(dt) &&
          request.equals(req)
      case _ => false
    }
  }

  override def toString: String = s"HubMessage($networkId, $operations, $source, $destinations, $data, $request)"

}

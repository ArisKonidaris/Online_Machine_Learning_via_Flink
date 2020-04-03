package oml.FlinkBipartiteAPI.messages

import java.io.Serializable

import oml.FlinkBipartiteAPI.POJOs.Request
import oml.StarTopologyAPI.operations.RemoteCallIdentifier
import oml.StarTopologyAPI.sites.NodeId

/** A message send by a Spoke node to a Hub node in the Flink Bipartite Network topology workflow.
  *
  * @param networkId   The id of the network that this message is referring to.
  * @param operation   The operation to be executed on the received Hub.
  * @param source      The source of the Spoke Message.
  * @param destination The destination of the Spoke Message.
  * @param data        The transferred data.
  * @param request     A serializable Request with all the necessary information
  *                    to configure the functionality of a Hub.
  */
case class SpokeMessage(var networkId: Int,
                        var operation: RemoteCallIdentifier,
                        var source: NodeId,
                        var destination: NodeId,
                        var data: Serializable,
                        var request: Request)
  extends FlinkMessage with Serializable {

  def this() = this(
    -1,
    new RemoteCallIdentifier(),
    new NodeId(null, -1),
    new NodeId(null, -1), null, new Request())

  def setNetworkId(networkId: Int): Unit = this.networkId = networkId

  def setOperation(operation: RemoteCallIdentifier): Unit = this.operation = operation

  def setSource(source: NodeId): Unit = this.source = source

  def setDestination(destination: NodeId): Unit = this.destination = destination

  def setData(data: Serializable): Unit = this.data = data

  def setRequest(request: Request): Unit = this.request = request

  def getNetworkId: Int = networkId

  def getOperation: RemoteCallIdentifier = operation

  def getSource: NodeId = source

  def getDestination: NodeId = destination

  def getData: Serializable = data

  def getRequest: Request = request

  override def equals(obj: Any): Boolean = {
    obj match {
      case ControlMessage(net, op, src, dst, dt, req) =>
        networkId == net &&
          operation.equals(op) &&
          source == src &&
          destination == dst &&
          data.equals(dt) &&
          request.equals(req)
      case _ => false
    }
  }

  override def toString: String = s"SpokeMessage($networkId, $operation, $source, $destination, $data, $request)"

}

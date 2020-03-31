package oml.FlinkBipartiteAPI.messages

import java.io.Serializable

import oml.FlinkBipartiteAPI.POJOs.Request
import oml.StarTopologyAPI.operations.RemoteCallIdentifier
import oml.StarTopologyAPI.sites.NodeId

case class WorkerMessage(override var networkId: Int,
                         override var operation: RemoteCallIdentifier,
                         override var source: NodeId,
                         override var destination: NodeId,
                         override var data: Serializable,
                         override var request: Request)
  extends ControlMessage {

  def this() = this(_, _, _, _, _, _)

  override def equals(obj: Any): Boolean = {
    obj match {
      case WorkerMessage(net, op, src, dst, dt, req) =>
        networkId == net &&
          operation.equals(op) &&
          source == src &&
          destination == dst &&
          data.equals(dt) &&
          request.equals(req)
      case _ => false
    }
  }

  override def toString: String = s"WorkerMessage($networkId, $operation, $source, $destination, $data, $request)"

}

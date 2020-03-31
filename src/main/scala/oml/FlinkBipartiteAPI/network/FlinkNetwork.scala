package oml.FlinkBipartiteAPI.network

import java.io.Serializable

import oml.FlinkBipartiteAPI.POJOs.QueryResponse
import oml.FlinkBipartiteAPI.messages.{ControlMessage, WorkerMessage}
import oml.OML_Job.queryResponse
import oml.StarTopologyAPI.network.Network
import oml.StarTopologyAPI.operations.RemoteCallIdentifier
import oml.StarTopologyAPI.sites.{NetworkDescriptor, NodeId, NodeType}
import oml.mlAPI.math.Point
import org.apache.flink.streaming.api.functions.co.CoProcessFunction
import org.apache.flink.util.Collector

case class FlinkNetwork(private var nodeType: NodeType,
                        private var networkId: Int,
                        private var numberOfSpokes: Int,
                        private var numberOfHubs: Int) extends Network {

  private var collector: Collector[WorkerMessage] = _
  private var context: CoProcessFunction[Point, ControlMessage, WorkerMessage]#Context = _

  def setNodeType(nodeType: NodeType): Unit = this.nodeType = nodeType

  def setNetworkID(networkId: Int): Unit = this.networkId = networkId

  def setNumberOfSpokes(numberOfSpokes: Int): Unit = this.numberOfSpokes = numberOfSpokes

  def setNumberOfHubs(numberOfHubs: Int): Unit = this.numberOfHubs = numberOfHubs

  def setCollector(collector: Collector[WorkerMessage]): Unit = this.collector = collector

  def setContext(context: CoProcessFunction[Point, ControlMessage, WorkerMessage]#Context): Unit = {
    this.context = context
  }

  override def send(source: NodeId, destination: NodeId, rpc: RemoteCallIdentifier, message: Serializable): Unit = {
    destination match {
      case null => context.output(queryResponse, message.asInstanceOf[QueryResponse])
      case dest: NodeId => collector.collect(WorkerMessage(networkId, rpc, source, dest, message, null))
    }
  }

  override def broadcast(source: NodeId, rpc: RemoteCallIdentifier, message: Serializable): Unit = {
    for (i <- 0 until (if (nodeType.equals(NodeType.HUB)) numberOfSpokes else numberOfHubs)) {
      val dest = new NodeId(if (nodeType.equals(NodeType.HUB)) NodeType.SPOKE else NodeType.HUB, i)
      collector.collect(WorkerMessage(networkId, rpc, source, dest, message, null))
    }
  }

  override def describe(): NetworkDescriptor = new NetworkDescriptor(networkId, numberOfSpokes, numberOfHubs)

}

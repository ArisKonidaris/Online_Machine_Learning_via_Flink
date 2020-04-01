package oml.FlinkBipartiteAPI.network

import java.io.Serializable

import oml.FlinkBipartiteAPI.POJOs._
import oml.FlinkBipartiteAPI.messages.{ControlMessage, SpokeMessage}
import oml.OML_Job.queryResponse
import oml.StarTopologyAPI.network.Network
import oml.StarTopologyAPI.operations.RemoteCallIdentifier
import oml.StarTopologyAPI.sites.{NetworkDescriptor, NodeId, NodeType}
import org.apache.flink.streaming.api.functions.KeyedProcessFunction
import org.apache.flink.streaming.api.functions.co.CoProcessFunction
import org.apache.flink.util.Collector

case class FlinkNetwork[InMsg <: Serializable, CtrlMsg <: Serializable, OutMsg <: Serializable]
(private var nodeType: NodeType,
 private var networkId: Int,
 private var numberOfSpokes: Int,
 private var numberOfHubs: Int) extends Network {

  private var collector: Collector[OutMsg] = _
  private var context: CoProcessFunction[InMsg, CtrlMsg, OutMsg]#Context = _
  private var keyedContext: KeyedProcessFunction[String, InMsg, OutMsg]#Context = _

  def setNodeType(nodeType: NodeType): Unit = this.nodeType = nodeType

  def setNetworkID(networkId: Int): Unit = this.networkId = networkId

  def setNumberOfSpokes(numberOfSpokes: Int): Unit = this.numberOfSpokes = numberOfSpokes

  def setNumberOfHubs(numberOfHubs: Int): Unit = this.numberOfHubs = numberOfHubs

  def setCollector(collector: Collector[OutMsg]): Unit = this.collector = collector

  def setContext(context: CoProcessFunction[InMsg, CtrlMsg, OutMsg]#Context): Unit = {
    this.context = context
  }

  def setKeyedContext(keyedContext: KeyedProcessFunction[String, InMsg, OutMsg]#Context): Unit = {
    this.keyedContext = keyedContext
  }

  override def send(source: NodeId, destination: NodeId, rpc: RemoteCallIdentifier, message: Serializable): Unit = {

    destination match {
      case null =>
        nodeType match {
          case NodeType.SPOKE => context.output(queryResponse, message.asInstanceOf[QueryResponse])
          case NodeType.HUB => keyedContext.output(queryResponse, message.asInstanceOf[QueryResponse])
        }
      case dest: NodeId =>
        nodeType match {
          case NodeType.SPOKE =>
            collector.collect(SpokeMessage(networkId, rpc, source, dest, message, null).asInstanceOf[OutMsg])
          case NodeType.HUB =>
            collector.collect(ControlMessage(networkId, rpc, source, destination, message, null).asInstanceOf[OutMsg])
        }
    }
  }

  override def broadcast(source: NodeId, rpc: RemoteCallIdentifier, message: Serializable): Unit = {
    for (i <- 0 until (if (nodeType.equals(NodeType.HUB)) numberOfSpokes else numberOfHubs)) {
      val dest = new NodeId(if (nodeType.equals(NodeType.HUB)) NodeType.SPOKE else NodeType.HUB, i)
      nodeType match {
        case NodeType.SPOKE =>
          collector.collect(SpokeMessage(networkId, rpc, source, dest, message, null).asInstanceOf[OutMsg])
        case NodeType.HUB =>
          collector.collect(ControlMessage(networkId, rpc, source, dest, message, null).asInstanceOf[OutMsg])
      }
    }
  }

  override def describe(): NetworkDescriptor = new NetworkDescriptor(networkId, numberOfSpokes, numberOfHubs)

}

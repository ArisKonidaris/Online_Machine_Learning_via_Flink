package oml.FlinkBipartiteAPI.network

import java.io.Serializable

import oml.FlinkBipartiteAPI.POJOs._
import oml.FlinkBipartiteAPI.messages.{ControlMessage, HubMessage, SpokeMessage}
import oml.OML_Job.queryResponse
import oml.StarTopologyAPI.network.Network
import oml.StarTopologyAPI.operations.RemoteCallIdentifier
import oml.StarTopologyAPI.sites.{NetworkDescriptor, NodeId, NodeType}
import org.apache.flink.streaming.api.functions.KeyedProcessFunction
import org.apache.flink.streaming.api.functions.co.CoProcessFunction
import org.apache.flink.util.Collector

import scala.collection.JavaConverters._
import scala.collection.immutable.HashMap
import scala.collection.mutable
import scala.collection.mutable.ListBuffer

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
          case NodeType.SPOKE =>
            context.output(queryResponse, message.asInstanceOf[Array[Any]](0).asInstanceOf[QueryResponse])
          case NodeType.HUB =>
            keyedContext.output(queryResponse, message.asInstanceOf[Array[Any]](0).asInstanceOf[QueryResponse])
        }
      case dest: NodeId =>
        nodeType match {
          case NodeType.SPOKE =>
            collector.collect(SpokeMessage(networkId, rpc, source, dest, message, null).asInstanceOf[OutMsg])
          case NodeType.HUB =>
            collector.collect(
              HubMessage(networkId,
                Array(rpc),
                source,
                Array(dest),
                message,
                null
              ).asInstanceOf[OutMsg]
            )
        }
    }
  }

  override def broadcast(source: NodeId, rpcs: java.util.Map[NodeId, RemoteCallIdentifier], message: Serializable)
  : Unit = {
    nodeType match {
      case NodeType.SPOKE =>
        for ((dest, rpc) <- rpcs.asScala)
          collector.collect(SpokeMessage(networkId, rpc, source, dest, message, null).asInstanceOf[OutMsg])
      case NodeType.HUB =>
        val destinations: ListBuffer[NodeId] = ListBuffer()
        val operations: ListBuffer[RemoteCallIdentifier] = ListBuffer()
         for ((dest,rpc) <- rpcs.asScala){
           destinations.append(dest)
           operations.append(rpc)
         }
        collector.collect(
          HubMessage(networkId, operations.toArray, source, destinations.toArray,message, null).asInstanceOf[OutMsg]
        )
    }
  }

  override def describe(): NetworkDescriptor = new NetworkDescriptor(networkId, numberOfSpokes, numberOfHubs)

}

package oml.FlinkBipartiteAPI.operators

import oml.FlinkBipartiteAPI.POJOs.Request
import oml.FlinkBipartiteAPI.state.{NodeAccumulator, NodeAggregateFunction}
import oml.FlinkBipartiteAPI.messages.{ControlMessage, SpokeMessage}
import oml.FlinkBipartiteAPI.network.FlinkNetwork
import oml.FlinkBipartiteAPI.nodes.hub.HubLogic
import oml.StarTopologyAPI.sites.{NodeId, NodeType}
import oml.StarTopologyAPI.{GenericWrapper, NodeGenerator}
import org.apache.flink.api.common.state._
import org.apache.flink.configuration.Configuration
import org.apache.flink.streaming.api.functions.KeyedProcessFunction
import org.apache.flink.streaming.api.scala.createTypeInformation
import org.apache.flink.util.Collector

import scala.reflect.Manifest

class FlinkHub[G <: NodeGenerator](implicit man: Manifest[G])
  extends HubLogic[SpokeMessage, ControlMessage] {

  override protected var state: AggregatingState[SpokeMessage, GenericWrapper] = _

  private var flinkNetwork: FlinkNetwork[SpokeMessage, ControlMessage, ControlMessage] = _

  override def open(parameters: Configuration): Unit = {
    state = getRuntimeContext.getAggregatingState[SpokeMessage, NodeAccumulator, GenericWrapper](
      new AggregatingStateDescriptor(
        "state",
        new NodeAggregateFunction(),
        createTypeInformation[NodeAccumulator]))
  }

  override def processElement(workerMessage: SpokeMessage,
                              ctx: KeyedProcessFunction[String, SpokeMessage, ControlMessage]#Context,
                              out: Collector[ControlMessage]): Unit = {

    workerMessage match {
      case SpokeMessage(network, operation, source, destination, data, request) =>
        request match {
          case req: Request =>
            req.getRequest match {
              case "Create" => generateHub(workerMessage, ctx, out)
              case "Update" =>
              case "Query" =>
              case "Delete" => state.clear()
              case _: String =>
                throw new RuntimeException(s"Unsupported request on Hub ${network + "_" + ctx.getCurrentKey}.")
            }
          case null =>
            if (flinkNetwork == null) {
              cache.append(workerMessage)
            } else {
              renewCollector(ctx, out)
              if (!cache.isEmpty) {
                while (cache.nonEmpty) {
                  val mess = cache.pop().get
                  state add mess
                }
              }
              state add workerMessage
            }
        }
    }
  }

  private def nodeFactory: NodeGenerator = man.runtimeClass.newInstance().asInstanceOf[NodeGenerator]

  private def renewCollector(ctx: KeyedProcessFunction[String, SpokeMessage, ControlMessage]#Context,
                             out: Collector[ControlMessage]): Unit = {
    flinkNetwork.setCollector(out)
    flinkNetwork.setKeyedContext(ctx)
  }

  private def generateHub(message: SpokeMessage,
                          ctx: KeyedProcessFunction[String, SpokeMessage, ControlMessage]#Context,
                          out: Collector[ControlMessage]): Unit = {
    val request: Request = message.getRequest
    val networkId: Int = message.getNetworkId
    val hubId: NodeId = new NodeId(NodeType.HUB, message.getDestination.getNodeId)
    flinkNetwork = FlinkNetwork[SpokeMessage, ControlMessage, ControlMessage](
      NodeType.HUB,
      networkId,
      getRuntimeContext.getExecutionConfig.getParallelism,
      if (request.getTraining_configuration.containsKey("HubParallelism"))
        request.getTraining_configuration.get("HubParallelism").asInstanceOf[Int]
      else 1
    )
    renewCollector(ctx, out)
    val genWrapper = new GenericWrapper(hubId, nodeFactory.generateHubNode(request), flinkNetwork)
    state add SpokeMessage(0, null, null, null, genWrapper, null)

  }

}


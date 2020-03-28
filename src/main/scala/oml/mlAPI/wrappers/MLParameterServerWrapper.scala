package oml.mlAPI.wrappers

import oml.FlinkBipartiteAPI.POJOs.Request
import oml.StarTopologyAPI.BufferingWrapper
import oml.StarTopologyAPI.network.Network
import oml.StarTopologyAPI.sites.NodeId
import oml.mlAPI.mlParameterServers.{MLParameterServerGenerator, PullPush}
import oml.mlAPI.mlworkers.generators.MLWorkerGenerator
import oml.mlAPI.mlworkers.worker.MLWorker

/**
  * This is a class that wraps a Parameter Server that synchronizes
  * remote Online Machine Learning models on streaming data.
  */
case class MLParameterServerWrapper(var nodeIP: NodeId,
                                    var node: MLWorker[_],
                                    var net: Network,
                                    var remoteArity: Int)
  extends MLWrapper(nodeIP, node, net, remoteArity) {

  def this() = this(_, _, _, _)

  require(nodeIP.isHub, "A parameter server node must have a negative nodeId")

}

object MLParameterServerWrapper {

  def apply(nodeIP: NodeId,
            generator: MLParameterServerGenerator,
            net: Network,
            remoteArity: Int,
            request: Request): MLParameterServerWrapper = {
    val wrappedMLWorker: MLWorker[_] = generateMLWorker(generator, request)
    val mlWorker: BufferingWrapper = BufferingWrapper(nodeIP,
      wrappedMLWorker,
      net,
      remoteArity)
    if (nodeIP.getNodeId > 0) {
      mlWorker.broadcastProxy.asInstanceOf[PullPush].pullModel.to(wrappedMLWorker.updateModel)
      mlWorker.setProcessData(false)
    }
    mlWorker
  }

  def apply(networkId: Integer,
            nodeId: Integer,
            generator: MLWorkerGenerator,
            net: Network,
            remoteArity: Int,
            request: Request): MLParameterServerWrapper = {
    val newIP: NodeId = new NodeId(networkId, nodeId)
    apply(newIP, generator, net, remoteArity, request)
  }

  def apply(nodeIP: NodeId,
            generator: MLWorkerGenerator,
            net: Network,
            remoteArity: Int,
            request: Request,
            processData: Boolean): MLParameterServerWrapper = {
    val wrappedMLWorker: MLWorker[_] = generateMLWorker(generator, request)
    val mlWorker: BufferingWrapper = BufferingWrapper(nodeIP,
      wrappedMLWorker,
      net,
      remoteArity)
    if (!processData) {
      mlWorker.broadcastProxy.asInstanceOf[PullPush].pullModel.to(wrappedMLWorker.updateModel)
      mlWorker.setProcessData(processData)
    }
    mlWorker
  }

  def apply(networkId: Integer,
            nodeId: Integer,
            generator: MLWorkerGenerator,
            net: Network,
            remoteArity: Int,
            request: Request,
            processData: Boolean): MLParameterServerWrapper = {
    val newIP: NodeId = new NodeId(networkId, nodeId)
    apply(newIP, generator, net, remoteArity, request, processData)
  }

  def generateMLWorker(generator: MLParameterServerGenerator, request: Request): MLWorker[_] =
    generator.generate(request).asInstanceOf[MLWorker[_]]

}
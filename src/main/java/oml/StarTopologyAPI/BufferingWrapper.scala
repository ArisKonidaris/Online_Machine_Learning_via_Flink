package oml.StarTopologyAPI

import java.io
import java.io.Serializable

import oml.FlinkAPI.POJOs.Request
import oml.StarTopologyAPI.network.{Network, Node}
import oml.StarTopologyAPI.operations.RemoteCallIdentifier
import oml.StarTopologyAPI.sites.NodeId
import oml.math.Point
import oml.mlAPI.dataBuffers.DataSet
import oml.mlAPI.mlParameterServers.PullPush
import oml.mlAPI.mlworkers.MLWorkerGenerator
import oml.mlAPI.mlworkers.worker.MLWorker

import scala.collection.mutable.ListBuffer

/**
  * This is a class that wraps a remote Site, or else a worker,
  * that trains a local Online Machine Learning model on streaming data.
  */
case class BufferingWrapper(var nodeIP: NodeId,
                            var node: MLWorker[_],
                            var net: Network,
                            var remoteArity: Int)
  extends MLWrapper(nodeIP, node, net, remoteArity) {

  protected var training_set: DataSet[Point] = new DataSet[Point]()

  protected var processData: Boolean = true

  def this() = this(_, _, _, _)

  require(nodeIP.isRemoteSite, "A worker node must have a non negative nodeId")

  override def receiveMsg(source: NodeId, rpc: RemoteCallIdentifier, tuple: Serializable): Unit = {

    // TODO: Do not forget to remove this after the implementation of the prediction job.
    if (source == null && tuple.isInstanceOf[ListBuffer[Point]]) {
      println(s"${getNodeId.getNodeId}, " +
        s"${getNodeId.getNodeType}, " +
        s"${node.getPerformance(tuple.asInstanceOf[ListBuffer[Point]])}, " +
        s"${training_set.length}, " +
        s"${tuple.asInstanceOf[ListBuffer[Point]].length}")
      return
    }

    super.receiveMsg(source, rpc, tuple)
    if (processData) {
      assert(training_set.isEmpty)
    } else {
      if (futures.isEmpty) {
        processData = true
        fitFromBuffer()
      }
    }
  }

  override def receiveTuple(tuple: io.Serializable): Unit = {
    if (!processData) {
      training_set.append(tuple.asInstanceOf[Point])
    } else if (!futures.isEmpty) {
      processData = false
      training_set.append(tuple.asInstanceOf[Point])
    } else {
      if (training_set.isEmpty) {
        super.receiveTuple(tuple)
      } else {
        training_set.append(tuple.asInstanceOf[Point])
        fitFromBuffer()
      }
    }
  }

  private def fitFromBuffer(): Unit = {
    while (futures.isEmpty && training_set.nonEmpty) {
      super.receiveTuple(training_set.remove(0))
    }
    if (!futures.isEmpty) processData = false
  }

  override def merge(nodes: Array[Node]): Unit = {
    for (node: Node <- nodes) {
      require(node.isInstanceOf[BufferingWrapper])
      setTrainingSet(training_set.merge(node.asInstanceOf[BufferingWrapper].getTrainingSet))
    }
    training_set.completeMerge()
    super.merge(nodes)
    broadcastProxy.asInstanceOf[PullPush].pullModel.to(node.updateModel)
    processData = false
  }

  def getTrainingSet: DataSet[Point] = training_set

  def setTrainingSet(training_set: DataSet[Point]): Unit = this.training_set = training_set

  def getProcessData: Boolean = processData

  def setProcessData(processData: Boolean): Unit = this.processData = processData

}

object BufferingWrapper {

  def apply(nodeIP: NodeId,
            generator: MLWorkerGenerator,
            net: Network,
            remoteArity: Int,
            request: Request): BufferingWrapper = {
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
            request: Request): BufferingWrapper = {
    val newIP: NodeId = new NodeId(networkId, nodeId)
    apply(newIP, generator, net, remoteArity, request)
  }

  def apply(nodeIP: NodeId,
            generator: MLWorkerGenerator,
            net: Network,
            remoteArity: Int,
            request: Request,
            processData: Boolean): BufferingWrapper = {
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
            processData: Boolean): BufferingWrapper = {
    val newIP: NodeId = new NodeId(networkId, nodeId)
    apply(newIP, generator, net, remoteArity, request, processData)
  }

  def generateMLWorker(generator: MLWorkerGenerator, request: Request): MLWorker[_] =
    generator.generate(request).asInstanceOf[MLWorker[_]]

}

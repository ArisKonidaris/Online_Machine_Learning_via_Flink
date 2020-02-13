package oml.message

import oml.message.packages.Container
import oml.parameters.LearningParameters

/** A control message send to the remote worker nodes of a
  * distributed star topology.
  *
  * @param workerID   Index of the Flink worker/partition
  * @param nodeID     The id of the local node to process
  * @param parameters The learning parameters
  * @param container  A serializable container with all the necessary information
  *                   to configure the functionality of a remote node
  */
case class ControlMessage(var request: Int,
                          var workerID: Int,
                          var nodeID: Int,
                          var parameters: Option[LearningParameters],
                          var container: Option[Container])
  extends Serializable {

  def this() = this(0, 0, 0, None, None)

  def setRequest(request: Int): Unit = this.request = request

  def setWorkerID(workerID: Int): Unit = this.workerID = workerID

  def setNodeID(pipelineID: Int): Unit = this.nodeID = pipelineID

  def setParameters(params: Option[LearningParameters]): Unit = parameters = params

  def setContainer(container: Option[Container]): Unit = this.container = container

  def getRequest: Int = request

  def getWorkerID: Int = workerID

  def getNodeID: Int = nodeID

  def getParameters: Option[LearningParameters] = parameters

  def getContainer: Option[Container] = container


  override def equals(obj: Any): Boolean = {
    obj match {
      case ControlMessage(req, wID, pID, params, cont) =>
        request.equals(req) &&
          workerID == wID &&
          nodeID == pID &&
          parameters.equals(params) &&
          container.equals(cont)
      case _ => false
    }
  }

  override def toString: String = {
    s"psMessage($request, $workerID, $nodeID, $parameters, $container)"
  }

}
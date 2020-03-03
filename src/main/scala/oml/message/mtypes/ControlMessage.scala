package oml.message.mtypes

import java.io.Serializable

import oml.POJOs.Request
import oml.parameters.LearningParameters

/** A control message send to the remote worker nodes of a
  * distributed star topology.
  *
  * @param workerID   Index of the Flink worker/partition
  * @param nodeID     The flink_worker_id of the local node to process
  * @param parameters The learning parameters
  * @param container  A serializable Request with all the necessary information
  *                   to configure the functionality of a remote node
  */
case class ControlMessage(var request: Option[Int],
                          var workerID: Int,
                          var nodeID: Int,
                          var parameters: Option[LearningParameters],
                          var container: Option[Request])
  extends Serializable {

  def this() = this(None, 0, 0, None, None)

  def setRequest(request: Int): Unit = this.request = Some(request)

  def setWorkerID(workerID: Int): Unit = this.workerID = workerID

  def setNodeID(pipelineID: Int): Unit = this.nodeID = pipelineID

  def setParameters(params: Option[LearningParameters]): Unit = parameters = params

  def setContainer(container: Option[Request]): Unit = this.container = container

  def getRequest: Option[Int] = request

  def getWorkerID: Int = workerID

  def getNodeID: Int = nodeID

  def getParameters: Option[LearningParameters] = parameters

  def getContainer: Option[Request] = container


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

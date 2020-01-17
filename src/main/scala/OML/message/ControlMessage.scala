package OML.message

import OML.message.packages.{Container, RequestType, UpdatePipelinePS}
import OML.parameters.LearningParameters

/** The new model hyperparameters send by the coordinator to a worker
  *
  * @param workerID   Index of the worker/partition
  * @param pipelineID The id of the ML pipeline to process
  * @param parameters The learning hyperparameters
  */
case class ControlMessage(var request: RequestType,
                          var workerID: Int,
                          var pipelineID: Int,
                          var parameters: Option[LearningParameters],
                          var container: Option[Container])
  extends Serializable {

  def this() = this(UpdatePipelinePS, 0, 0, None, None)

  def getRequest: RequestType = request

  def setRequest(request: RequestType): Unit = this.request = request

  def getWorkerID: Int = workerID

  def setWorkerID(workerID: Int): Unit = this.workerID = workerID

  def getPipelineID: Int = pipelineID

  def setPipelineID(pipelineID: Int): Unit = this.pipelineID = pipelineID

  def getParameters: Option[LearningParameters] = parameters

  def setParameters(params: Option[LearningParameters]): Unit = parameters = params

  def getContainer: Option[Container] = container

  def setContainer(container: Option[Container]): Unit = this.container = container

  override def equals(obj: Any): Boolean = {
    obj match {
      case ControlMessage(req, wID, pID, params, cont) =>
        request.equals(req) &&
          workerID == wID &&
          pipelineID == pID &&
          parameters.equals(params) &&
          container.equals(cont)
      case _ => false
    }
  }

  override def toString: String = {
    s"psMessage($request, $workerID, $pipelineID, $parameters, $container)"
  }

}
package oml.message.mtypes

case class workerMessage(var nodeID: Int, var workerId: Int, var parameters: java.io.Serializable, var request: Int)
  extends Serializable {

  def this() = this(0, 0, null, 0)

  def this(nID: Int, wID: Int) = this(nID, wID, null, 0)

  def getPipelineID: Int = nodeID

  def setPipelineID(pipelineID: Int): Unit = this.nodeID = pipelineID

  def getWorkerId: Int = workerId

  def setWorkerId(id: Int): Unit = workerId = id

  def getParameters: java.io.Serializable = parameters

  def setParameters(params: java.io.Serializable): Unit = parameters = params

  override def equals(obj: Any): Boolean = {
    obj match {
      case workerMessage(pID, wID, params, req) =>
        nodeID == pID &&
          workerId == wID &&
          parameters.equals(params) &&
          request == req
      case _ => false
    }
  }

  override def toString: String = {
    s"workerMessage($nodeID, $workerId, $parameters, $request)"
  }

}

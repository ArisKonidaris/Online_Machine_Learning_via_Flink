package oml.message

import breeze.linalg.{DenseVector => BreezeDenseVector}
import oml.parameters.{LearningParameters, LinearModelParameters}

case class workerMessage(var nodeID: Int, var workerId: Int, var parameters: LearningParameters, var request: Int)
  extends Serializable {

  def this() = this(0, 0, LinearModelParameters(BreezeDenseVector.zeros[Double](0), 0.0), 0)

  def this(nID: Int, wID: Int) = this(nID, wID, LinearModelParameters(BreezeDenseVector.zeros[Double](0), 0.0), 0)

  def getPipelineID: Int = nodeID

  def setPipelineID(pipelineID: Int): Unit = this.nodeID = pipelineID

  def getWorkerId: Int = workerId

  def setWorkerId(id: Int): Unit = workerId = id

  def getParameters: LearningParameters = parameters

  def setParameters(params: LearningParameters): Unit = parameters = params

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
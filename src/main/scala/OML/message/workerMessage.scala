package OML.message

import OML.parameters.{LearningParameters, LinearModelParameters}
import breeze.linalg.{DenseVector => BreezeDenseVector}

case class workerMessage(var pipelineID: Int, var workerId: Int, var parameters: LearningParameters, var request: Int)
  extends Serializable {

  def this() = this(0, 0, LinearModelParameters(BreezeDenseVector.zeros[Double](0), 0.0), 0)

  def this(pID: Int, wID: Int) = this(pID, wID, LinearModelParameters(BreezeDenseVector.zeros[Double](0), 0.0), 0)

  def getPipelineID: Int = pipelineID

  def setPipelineID(pipelineID: Int): Unit = this.pipelineID = pipelineID

  def getWorkerId: Int = workerId

  def setWorkerId(id: Int): Unit = workerId = id

  def getParameters: LearningParameters = parameters

  def setParameters(params: LearningParameters): Unit = parameters = params

  override def equals(obj: Any): Boolean = {
    obj match {
      case workerMessage(pID, wID, params, req) =>
        pipelineID == pID &&
          workerId == wID &&
          parameters.equals(params) &&
          request == req
      case _ => false
    }
  }

  override def toString: String = {
    s"workerMessage($pipelineID, $workerId, $parameters, $request)"
  }

}

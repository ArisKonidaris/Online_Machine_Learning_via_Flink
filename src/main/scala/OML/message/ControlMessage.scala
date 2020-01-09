package OML.message

import OML.parameters.{LearningParameters, LinearModelParameters}
import breeze.linalg.{DenseVector => BreezeDenseVector}

/** The new model parameters send by the coordinator to a worker
  *
  * @param workerID   Index of the worker/partition
  * @param pipelineID The id of the ML pipeline to process
  * @param parameters The learning parameters
  */
case class ControlMessage(var workerID: Int, var pipelineID: Int, var parameters: LearningParameters)
  extends Serializable {

  def this() = this(0, 0, LinearModelParameters(BreezeDenseVector.zeros[Double](0), 0.0))

  def getWorkerID: Int = workerID

  def setWorkerID(workerID: Int): Unit = this.workerID = workerID

  def getPipelineID: Int = pipelineID

  def setPipelineID(pipelineID: Int): Unit = this.pipelineID = pipelineID

  def getParameters: LearningParameters = parameters

  def setParameters(params: LearningParameters): Unit = parameters = params

  override def equals(obj: Any): Boolean = {
    obj match {
      case ControlMessage(wID, pID, params) =>
        workerID == wID &&
          pipelineID == pID &&
          parameters.equals(params)
      case _ => false
    }
  }

  override def toString: String = {
    s"psMessage($workerID, $pipelineID, $parameters)"
  }

}

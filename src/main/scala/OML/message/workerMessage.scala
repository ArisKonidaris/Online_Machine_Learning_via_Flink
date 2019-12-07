package OML.message

import OML.parameters.{LearningParameters, LinearModelParameters}
import breeze.linalg.{DenseVector => BreezeDenseVector}

case class workerMessage(var partition: Int, var workerId: Int, var parameters: LearningParameters, var request: Int)
  extends Serializable {

  def this() = this(0, 0, LinearModelParameters(BreezeDenseVector.zeros[Double](0), 0.0), 0)

  def getPartition: Int = partition

  def setPartition(partition: Int): Unit = this.partition = partition

  def getWorkerId: Int = workerId

  def setWorkerId(id: Int): Unit = workerId = id

  def getParameters: LearningParameters = parameters

  def setParameters(params: LearningParameters): Unit = parameters = params

  override def equals(obj: Any): Boolean = {
    obj match {
      case workerMessage(part, id, params, req) => partition == part &&
        workerId == id &&
        parameters.equals(params) &&
        request == req
      case _ => false
    }
  }

  override def toString: String = {
    s"workerMessage($partition, $workerId, $parameters)"
  }

}

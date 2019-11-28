package OML.message

import OML.parameters.{LearningParameters, LinearModelParameters}
import breeze.linalg.{DenseVector => BreezeDenseVector}

case class workerMessage(part: Int, var workerId: Int, var data: LearningParameters) extends LearningMessage {

  def this() = this(0, 0, LinearModelParameters(BreezeDenseVector.zeros[Double](0), 0.0))

  var partition: Int = part

  def setPart(p: Int): Unit = partition = p

  def setWorkerId(id: Int): Unit = workerId = id

  def setData(dt: LearningParameters): Unit = data = dt

  override def equals(obj: Any): Boolean = {
    obj match {
      case workerMessage(part, id, params) => partition == part && workerId == id && data.equals(params)
      case _ => false
    }
  }

  override def toString: String = {
    s"workerMessage($partition, $workerId, $data)"
  }

}

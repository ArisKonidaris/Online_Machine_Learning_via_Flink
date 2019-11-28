package OML.message

import OML.parameters.{LearningParameters, LinearModelParameters}
import breeze.linalg.{DenseVector => BreezeDenseVector}

/** The new model parameters send by the coordinator to a worker
  *
  * @param part Index of the worker/partition
  * @param data The new parameters
  */
case class ControlMessage(part: Int, var data: LearningParameters) extends LearningMessage {

  def this() = this(0, LinearModelParameters(BreezeDenseVector.zeros[Double](0), 0.0))

  var partition: Int = part

  def setPart(p: Int): Unit = partition = p

  def setData(dt: LearningParameters): Unit = data = dt

  override def equals(obj: Any): Boolean = {
    obj match {
      case ControlMessage(part, params) => partition == part && data.equals(params)
      case _ => false
    }
  }

  override def toString: String = {
    s"psMessage($partition, $data)"
  }

}

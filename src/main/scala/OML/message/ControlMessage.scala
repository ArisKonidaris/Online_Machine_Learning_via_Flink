package OML.message

import OML.parameters.{LearningParameters, LinearModelParameters}
import breeze.linalg.{DenseVector => BreezeDenseVector}

/** The new model parameters send by the coordinator to a worker
  *
  * @param partition  Index of the worker/partition
  * @param parameters The learning parameters
  */
case class ControlMessage(var partition: Int, var parameters: LearningParameters) extends Serializable {

  def this() = this(0, LinearModelParameters(BreezeDenseVector.zeros[Double](0), 0.0))

  def getPartition: Int = partition

  def setPartition(partition: Int): Unit = this.partition = partition

  def getParameters: LearningParameters = parameters

  def setParameters(params: LearningParameters): Unit = parameters = params

  override def equals(obj: Any): Boolean = {
    obj match {
      case ControlMessage(part, params) => partition == part && parameters.equals(params)
      case _ => false
    }
  }

  override def toString: String = {
    s"psMessage($partition, $parameters)"
  }

}

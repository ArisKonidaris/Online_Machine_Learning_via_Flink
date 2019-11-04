package OML.message

import OML.parameters.LearningParameters


/** The new model parameters send by the coordinator to a worker
  *
  * @param partition Index of the worker/partition
  * @param data The new parameters
  */
case class psMessage(override val partition: Int, data: LearningParameters) extends ControlMessage {

  override def equals(obj: Any): Boolean = {
    obj match {
      case psMessage(part, params) => partition == part && data.equals(params)
      case _ => false
    }
  }

  override def toString: String = {
    s"psMessage($partition, $data)"
  }

}

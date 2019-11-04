package OML.protocol

import OML.learners.Learner
import OML.logic.{AsyncCoWorkerHack, RichAsyncPS}
import OML.message.{ControlMessage, DataPoint, LearningMessage}
import OML.parameters.LearningParameters

case class AsynchronousCoHackProto[L <: Learner : Manifest]()
  extends LearningCoProto[DataPoint, ControlMessage, (Int, Int, LearningParameters), ControlMessage] {
  override def workerLogic: AsyncCoWorkerHack[L] = new AsyncCoWorkerHack[L]

  override def psLogic: RichAsyncPS = new RichAsyncPS
}
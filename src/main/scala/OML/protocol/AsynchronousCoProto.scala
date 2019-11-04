package OML.protocol

import OML.learners.Learner
import OML.logic.{AsyncCoWorker, RichAsyncPS}
import OML.message.{ControlMessage, DataPoint, LearningMessage}
import OML.parameters.LearningParameters

case class AsynchronousCoProto[L <: Learner : Manifest]()
  extends LearningCoProto[DataPoint, ControlMessage, (Int, Int, LearningParameters), ControlMessage] {
  override def workerLogic: AsyncCoWorker[L] = new AsyncCoWorker[L]
  override def psLogic: RichAsyncPS = new RichAsyncPS
}

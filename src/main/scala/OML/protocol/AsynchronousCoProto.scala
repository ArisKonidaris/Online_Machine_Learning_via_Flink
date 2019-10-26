package OML.protocol

import OML.learners.Learner
import OML.logic.{AsyncCoWorker, RichAsyncPS}
import OML.message.LearningMessage
import OML.parameters.LearningParameters

case class AsynchronousCoProto[L <: Learner : Manifest]()
  extends LearningProto[LearningMessage, (Int, Int, LearningParameters), LearningMessage] {
  override def workerLogic: AsyncCoWorker[L] = new AsyncCoWorker[L]

  override def psLogic: RichAsyncPS = new RichAsyncPS
}

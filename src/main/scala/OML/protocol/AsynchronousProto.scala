package OML.protocol

import OML.learners.Learner
import OML.logic.{RichAsyncPS, AsyncWorker}
import OML.message.LearningMessage
import OML.parameters.LearningParameters

case class AsynchronousProto[L <: Learner : Manifest]()
  extends LearningProto[LearningMessage, (Int, Int, LearningParameters), LearningMessage, L] {
  override def workerLogic: AsyncWorker[L] = new AsyncWorker[L]
  override def psLogic: RichAsyncPS = new RichAsyncPS
}

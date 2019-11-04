package OML.protocol

import OML.learners.Learner
import OML.logic.{AsyncWorker, RichAsyncPS}
import OML.message.{ControlMessage, LearningMessage}
import OML.parameters.LearningParameters

case class AsynchronousProto[L <: Learner : Manifest]()
  extends LearningProto[LearningMessage, (Int, Int, LearningParameters), ControlMessage] {
  override def workerLogic: AsyncWorker[L] = new AsyncWorker[L]
  override def psLogic: RichAsyncPS = new RichAsyncPS
}

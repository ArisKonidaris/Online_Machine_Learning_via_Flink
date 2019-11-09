package OML.protocol

import OML.learners.Learner
import OML.logic.{AsyncWorker, RichAsyncPS}

case class AsynchronousProto[L <: Learner : Manifest]()
  extends LearningProto {
  override def workerLogic: AsyncWorker[L] = new AsyncWorker[L]
  override def psLogic: RichAsyncPS = new RichAsyncPS
}

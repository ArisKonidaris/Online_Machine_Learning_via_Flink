package OML.protocol

import OML.learners.Learner
import OML.logic.{AsyncCoWorker, RichAsyncPS}

case class AsynchronousCoProto[L <: Learner : Manifest]()
  extends LearningProto {
  override def workerLogic: AsyncCoWorker[L] = new AsyncCoWorker[L]
  override def psLogic: RichAsyncPS = new RichAsyncPS
}

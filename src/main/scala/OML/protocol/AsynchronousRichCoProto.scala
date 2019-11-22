package OML.protocol

import OML.learners.Learner
import OML.logic.{AsyncRichCoWorker, RichAsyncPS}

case class AsynchronousRichCoProto[L <: Learner : Manifest]()
  extends LearningProto {
  override def workerLogic: AsyncRichCoWorker[L] = new AsyncRichCoWorker[L]

  override def psLogic: RichAsyncPS = new RichAsyncPS
}

package OML.protocol

import OML.learners.Learner
import OML.logic.{AsyncCoWorkerHack, RichAsyncPS}

case class AsynchronousCoHackProto[L <: Learner : Manifest]()
  extends LearningProto {
  override def workerLogic: AsyncCoWorkerHack[L] = new AsyncCoWorkerHack[L]
  override def psLogic: RichAsyncPS = new RichAsyncPS
}
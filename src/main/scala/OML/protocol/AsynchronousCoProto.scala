package OML.protocol

import OML.learners.Learner
import OML.logic.{AsyncCoWorker, RichAsyncPS}

case class AsynchronousCoProto()
  extends LearningProto {
  override def workerLogic: AsyncCoWorker = new AsyncCoWorker

  override def psLogic: RichAsyncPS = new RichAsyncPS
}

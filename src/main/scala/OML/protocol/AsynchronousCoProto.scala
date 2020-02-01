package OML.protocol

import OML.logic.{AsyncCoWorker, RichAsyncPS}
import OML.mlAPI.learners.Learner

case class AsynchronousCoProto()
  extends LearningProto {
  override def workerLogic: AsyncCoWorker = new AsyncCoWorker

  override def psLogic: RichAsyncPS = new RichAsyncPS
}

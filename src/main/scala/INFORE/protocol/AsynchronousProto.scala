package INFORE.protocol

import INFORE.learners.Learner
import INFORE.logic.{RichAsyncPS, AsyncWorker}
import INFORE.message.LearningMessage
import INFORE.parameters.LearningParameters

case class AsynchronousProto[L <: Learner : Manifest]()
  extends LearningProto[LearningMessage, (Int, Int, LearningParameters), LearningMessage, L] {
  override def workerLogic: AsyncWorker[L] = new AsyncWorker[L]

  override def psLogic: RichAsyncPS = new RichAsyncPS
}

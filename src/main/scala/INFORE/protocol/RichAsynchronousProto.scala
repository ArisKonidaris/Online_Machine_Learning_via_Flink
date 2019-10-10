package INFORE.protocol

import INFORE.learners.Learner
import INFORE.logic.{RichAsyncPS, RichAsyncWorker}
import INFORE.message.LearningMessage
import INFORE.parameters.LearningParameters

case class RichAsynchronousProto[L <: Learner : Manifest]()
  extends safeLearningProto[LearningMessage, (Int, Int, LearningParameters), LearningMessage, L] {
  override def workerLogic: RichAsyncWorker[L] = new RichAsyncWorker[L]

  override def psLogic: RichAsyncPS = new RichAsyncPS
}


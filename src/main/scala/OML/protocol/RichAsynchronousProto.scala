package OML.protocol

import OML.learners.Learner
import OML.logic.{RichAsyncPS, RichAsyncWorker}
import OML.message.LearningMessage
import OML.parameters.LearningParameters

case class RichAsynchronousProto[L <: Learner : Manifest]()
  extends safeLearningProto[LearningMessage, (Int, Int, LearningParameters), LearningMessage, L] {
  override def workerLogic: RichAsyncWorker[L] = new RichAsyncWorker[L]

  override def psLogic: RichAsyncPS = new RichAsyncPS
}


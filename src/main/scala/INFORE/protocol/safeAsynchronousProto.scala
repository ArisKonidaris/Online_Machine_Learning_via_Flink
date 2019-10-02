package INFORE.protocol

import INFORE.learners.Learner
import INFORE.logic.{psAsyncLogic, safeWorkerAsyncLogic}
import INFORE.message.LearningMessage
import INFORE.parameters.LearningParameters

case class safeAsynchronousProto[L <: Learner : Manifest]()
  extends safeLearningProto[LearningMessage, (Int, Int, LearningParameters), LearningMessage, L] {
  override def workerLogic: safeWorkerAsyncLogic[L] = new safeWorkerAsyncLogic[L]
  override def psLogic: psAsyncLogic = new psAsyncLogic
}


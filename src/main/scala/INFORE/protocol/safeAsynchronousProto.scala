package INFORE.protocol

import INFORE.learners.Learner
import INFORE.logic.{psAsyncLogic, safeWorkerAsyncLogic, workerAsyncLogic}
import INFORE.message.LearningMessage
import INFORE.parameters.LearningParameters

class safeAsynchronousProto[L <: Learner]()
  extends safeLearningProto[LearningMessage, (Int, Int, LearningParameters), LearningMessage, L] {
  override def workerLogic: safeWorkerAsyncLogic[L] = new safeWorkerAsyncLogic[L]

  override def psLogic: psAsyncLogic = new psAsyncLogic
}


package INFORE.protocol

import INFORE.learners.Learner
import INFORE.logic.{psAsyncLogic, workerAsyncLogic}
import INFORE.message.LearningMessage
import INFORE.parameters.LearningParameters

case class AsynchronousProto[L <: Learner : Manifest]()
  extends LearningProto[LearningMessage, (Int, Int, LearningParameters), LearningMessage, L] {
  override def workerLogic: workerAsyncLogic[L] = new workerAsyncLogic[L]
  override def psLogic: psAsyncLogic = new psAsyncLogic
}

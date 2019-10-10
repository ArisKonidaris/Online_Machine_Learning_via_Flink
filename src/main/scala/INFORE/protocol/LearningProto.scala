package INFORE.protocol

import INFORE.learners.Learner
import INFORE.nodes.ParameterServerNode.RichPSLogic
import INFORE.nodes.WorkerNode.{RichWorkerLogic, WorkerLogic}

/** Base trait of a learning protocol
  *
  * @tparam T The input message type accepted by the worker
  * @tparam U The output message type the worker emits / the input message type accepted by the parameter server
  * @tparam A The output message type the parameter server emits
  * @tparam L The learner type
  */
trait LearningProto[T, U, A, L <: Learner] {
  def workerLogic: WorkerLogic[T, U, L]

  def psLogic: RichPSLogic[U, A]
}

trait safeLearningProto[T, U, A, L <: Learner] {
  def workerLogic: RichWorkerLogic[T, U, L]

  def psLogic: RichPSLogic[U, A]
}

package INFORE.protocol

import INFORE.nodes.ParameterServerNode.ParameterServerLogic
import INFORE.nodes.WorkerNode.{SafeWorkerLogic, WorkerLogic}

/** Base trait of a learning protocol
  *
  * @tparam T The input message type accepted by the worker
  * @tparam U The output message type the worker emits / the input message type accepted by the parameter server
  * @tparam A The output message type the parameter server emits
  * @tparam L The learner type
  */
trait LearningProto[T, U, A, L] {
  def workerLogic: WorkerLogic[T, U, L]
  def psLogic: ParameterServerLogic[U, A]
}

trait safeLearningProto[T, U, A, L] {
  def workerLogic: SafeWorkerLogic[T, U, L]
  def psLogic: ParameterServerLogic[U, A]
}

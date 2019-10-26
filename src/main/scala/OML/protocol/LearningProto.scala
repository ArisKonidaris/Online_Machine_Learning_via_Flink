package OML.protocol

import OML.learners.Learner
import OML.nodes.ParameterServerNode.RichPSLogic
import OML.nodes.WorkerNode.{Worker, WorkerLogic}

/** Base trait of a learning protocol
  *
  * @tparam T The input message type accepted by the worker
  * @tparam U The output message type the worker emits / the input message type accepted by the parameter server
  * @tparam A The output message type the parameter server emits
  */
trait LearningProto[T, U, A] {
  def workerLogic: Worker[T, U]
  def psLogic: RichPSLogic[U, A]
}

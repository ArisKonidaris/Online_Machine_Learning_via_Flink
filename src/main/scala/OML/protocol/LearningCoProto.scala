package OML.protocol

import OML.nodes.ParameterServerNode.RichPSLogic
import OML.nodes.WorkerNode.CoWorker

/** Base trait of a learning protocol
  *
  * @tparam T The input message type accepted by the worker
  * @tparam C The control message type accepted by the worker
  * @tparam U The output message type the worker emits / the input message type accepted by the parameter server
  * @tparam A The output message type the parameter server emits
  */
trait LearningCoProto[T, C, U, A] {
  def workerLogic: CoWorker[T, C, U]

  def psLogic: RichPSLogic[U, A]
}

package OML.nodes.WorkerNode

import OML.learners.Learner
import OML.parameters.{LearningParameters => lparams}
import org.apache.flink.util.Collector

/** Basic trait of a worker node in a distributed machine learning training setting
  *
  * @tparam InMsg  The input message type accepted by the worker
  * @tparam CntMsg The control message type accepted by the worker
  * @tparam OutMsg The output message type the worker emits
  */
trait CoWorker[InMsg, CntMsg, OutMsg] extends Serializable {
  var learner: Learner

  def updateLocalModel(data: lparams): Unit

  def checkIfMessageToServerIsNeeded(): Boolean

  def sendModelToServer(out: Collector[OutMsg]): Unit

  def setWorkerId(id: Int): Unit
}

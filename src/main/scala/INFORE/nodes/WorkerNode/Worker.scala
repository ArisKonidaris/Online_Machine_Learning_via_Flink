package INFORE.nodes.WorkerNode

import INFORE.learners.Learner
import INFORE.parameters.{LearningParameters => lparams}
import org.apache.flink.util.Collector

/** Basic trait of a worker node in a distributed machine learning training setting
  *
  * @tparam InMsg  The input message type accepted by the worker
  * @tparam OutMsg The output message type the worker emits
  */
trait Worker[InMsg, OutMsg] extends Serializable {
  var learner: Learner

  def updateLocalModel(data: lparams): Unit

  def checkIfMessageToServerIsNeeded(): Boolean

  def sendModelToServer(out: Collector[OutMsg]): Unit

  def setWorkerId(id: Int): Unit
}

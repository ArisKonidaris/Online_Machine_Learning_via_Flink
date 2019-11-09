package OML.nodes.WorkerNode

import OML.learners.Learner
import OML.parameters.{LearningParameters => lparams}
import org.apache.flink.api.common.functions.Function

/**
  * The basic trait for a Machine Learning worker node
  */
trait Worker extends Function with Serializable {
  var learner: Learner
  def updateLocalModel(data: lparams): Unit
  def checkIfMessageToServerIsNeeded(): Boolean
  def setWorkerId(id: Int): Unit
}

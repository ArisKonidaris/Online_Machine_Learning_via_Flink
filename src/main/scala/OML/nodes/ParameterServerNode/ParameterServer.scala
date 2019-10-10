package OML.nodes.ParameterServerNode

import OML.parameters.{LearningParameters => l_params}
import org.apache.flink.api.common.state.AggregatingState
import org.apache.flink.util.Collector

/** Basic trait of a parameter server for distributed machine learning training
  *
  * @tparam InMsg  The input message type accepted by the parameter server
  * @tparam OutMsg The output message type the parameter server emits
  */
trait ParameterServer[InMsg, OutMsg] extends Serializable {
  def receiveMessage(in: InMsg, collector: Collector[OutMsg]): Unit

  def sendMessage(id: Int, collector: Collector[OutMsg]): Unit

  def updateGlobalModel(localModel: l_params, k: Int)(implicit gModel: AggregatingState[l_params, l_params]): Unit
}
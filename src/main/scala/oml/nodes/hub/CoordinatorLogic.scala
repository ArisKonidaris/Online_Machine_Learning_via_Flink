package oml.nodes.hub

import oml.mlAPI.parameters.LearningParameters
import org.apache.flink.api.common.functions.RichFlatMapFunction
import org.apache.flink.util.Collector

/** Basic abstract logic of a coordinator request Flink.
  *
  * @tparam InMsg  The input message type accepted by the coordinator
  * @tparam OutMsg The output message type emitted by the coordinator
  */
abstract class CoordinatorLogic[InMsg, OutMsg]
  extends RichFlatMapFunction[InMsg, OutMsg]
    with Coordinator {
  def updateGlobalState(localModel: LearningParameters): Unit

  def sendMessage(siteID: Int, collector: Collector[OutMsg]): Unit
}

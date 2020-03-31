package oml.FlinkBipartiteAPI.nodes.hub

import oml.mlAPI.parameters.LearningParameters
import org.apache.flink.api.common.functions.RichFlatMapFunction
import org.apache.flink.util.Collector

/** Basic abstract operator of a coordinator in Flink.
  *
  * @tparam InMsg  The message message type accepted by the coordinator
  * @tparam OutMsg The output message type emitted by the coordinator
  */
abstract class HubLogic[InMsg, OutMsg]
  extends RichFlatMapFunction[InMsg, OutMsg]
    with Hub {
  def updateGlobalState(localModel: LearningParameters): Unit

  def sendMessage(siteID: Int, collector: Collector[OutMsg]): Unit
}

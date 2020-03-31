package oml.FlinkBipartiteAPI.nodes.hub

import oml.StarTopologyAPI.GenericWrapper
import org.apache.flink.api.common.state.AggregatingState
import org.apache.flink.streaming.api.functions.KeyedProcessFunction

/** Basic abstract operator of a coordinator in Flink.
  *
  * @tparam InMsg  The worker message type accepted by the coordinator.
  * @tparam OutMsg The output message type emitted by the coordinator.
  */
abstract class HubLogic[InMsg, OutMsg]
  extends KeyedProcessFunction[String, InMsg, OutMsg]
    with Hub {
  protected var state: AggregatingState[InMsg, GenericWrapper]
}

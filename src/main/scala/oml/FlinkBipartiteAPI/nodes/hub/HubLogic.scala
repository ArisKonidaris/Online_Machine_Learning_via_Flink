package oml.FlinkBipartiteAPI.nodes.hub

import java.io.Serializable

import oml.StarTopologyAPI.GenericWrapper
import oml.mlAPI.dataBuffers.DataSet
import org.apache.flink.api.common.state.AggregatingState
import org.apache.flink.streaming.api.functions.KeyedProcessFunction

/** Basic abstract operator of a coordinator in Flink.
  *
  * @tparam InMsg  The worker message type accepted by the coordinator.
  * @tparam OutMsg The output message type emitted by the coordinator.
  */
abstract class HubLogic[InMsg <: Serializable, OutMsg <: Serializable]
  extends KeyedProcessFunction[String, InMsg, OutMsg]
    with Hub {
  protected var state: AggregatingState[InMsg, GenericWrapper]
  protected var cache: DataSet[InMsg] = new DataSet[InMsg](20000)
}

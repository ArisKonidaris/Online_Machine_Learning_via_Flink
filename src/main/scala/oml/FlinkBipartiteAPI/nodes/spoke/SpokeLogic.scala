package oml.FlinkBipartiteAPI.nodes.spoke

import java.io.Serializable

import oml.StarTopologyAPI.network.Mergeable
import oml.StarTopologyAPI.BufferingWrapper
import oml.mlAPI.dataBuffers.DataSet
import org.apache.flink.api.common.state.ListState
import org.apache.flink.streaming.api.checkpoint.CheckpointedFunction
import org.apache.flink.streaming.api.functions.co.CoProcessFunction

import scala.collection.mutable.ListBuffer

/** An abstract operators of a stateful remote spoke.
  *
  * @tparam InMsg   The message message type accepted by the spoke
  * @tparam CtrlMsg The control message send by the coordinator to the remote sites
  * @tparam OutMsg  The output message type emitted by the spoke
  */
abstract class SpokeLogic[InMsg <: Serializable, CtrlMsg <: Serializable, OutMsg <: Serializable]
  extends CoProcessFunction[InMsg, CtrlMsg, OutMsg]
    with CheckpointedFunction
    with Spoke {

  var state: scala.collection.mutable.Map[Int, BufferingWrapper[InMsg]] =
    scala.collection.mutable.Map[Int, BufferingWrapper[InMsg]]()
  var cache: DataSet[InMsg] = new DataSet[InMsg](20000)

  def mergingDataBuffers(saved_buffers: ListState[DataSet[InMsg]]): DataSet[InMsg] = {
    var new_buffer = new DataSet[InMsg]()

    val buffers_iterator = saved_buffers.get.iterator
    val buffers: ListBuffer[Mergeable] = ListBuffer[Mergeable]()
    while (buffers_iterator.hasNext) {
      val next = buffers_iterator.next
      if (next.nonEmpty)
        if (new_buffer.isEmpty) new_buffer = next else buffers.append(next)
    }
    new_buffer.merge(buffers.toArray)

    new_buffer
  }

}

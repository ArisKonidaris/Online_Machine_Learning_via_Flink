package oml.nodes.site

import oml.StarProtocolAPI.GenericWrapper
import oml.math.Point
import oml.mlAPI.dataBuffers.DataSet
import org.apache.flink.streaming.api.checkpoint.CheckpointedFunction
import org.apache.flink.streaming.api.functions.co.RichCoFlatMapFunction

/** An abstract logic of a stateful remote site.
  *
  * @tparam InMsg   The input message type accepted by the site
  * @tparam CtrlMsg The control message send by the coordinator to the remote sites
  * @tparam OutMsg  The output message type emitted by the site
  */
abstract class SiteLogic[InMsg, CtrlMsg, OutMsg]
  extends RichCoFlatMapFunction[InMsg, CtrlMsg, OutMsg]
    with CheckpointedFunction
    with Site {
  var state: scala.collection.mutable.Map[Int, GenericWrapper] = scala.collection.mutable.Map[Int, GenericWrapper]()
  var cache: DataSet[Point] = new DataSet[Point](20000)
}

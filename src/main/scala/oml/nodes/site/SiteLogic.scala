package oml.nodes.site

import oml.StarProtocolAPI.Node
import org.apache.flink.streaming.api.checkpoint.CheckpointedFunction
import org.apache.flink.streaming.api.functions.co.CoFlatMapFunction

/** An abstract logic of a stateful remote site.
  *
  * @tparam InMsg   The input message type accepted by the site
  * @tparam CtrlMsg The control message send by the coordinator to the remote sites
  * @tparam OutMsg  The output message type emitted by the site
  */
abstract class SiteLogic[InMsg, CtrlMsg, OutMsg]
  extends CoFlatMapFunction[InMsg, CtrlMsg, OutMsg]
    with CheckpointedFunction
    with Site {
  var state: scala.collection.mutable.Map[Int, Node] = scala.collection.mutable.Map[Int, Node]()
}

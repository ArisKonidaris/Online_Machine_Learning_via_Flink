package OML.nodes.site

import OML.mlAPI.MLWorker
import OML.parameters.LearningParameters
import org.apache.flink.streaming.api.checkpoint.CheckpointedFunction
import org.apache.flink.streaming.api.functions.co.CoFlatMapFunction
import org.apache.flink.util.Collector

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
  var state: scala.collection.mutable.Map[Int, MLWorker] = scala.collection.mutable.Map[Int, MLWorker]()

  def updateState(stateID: Int, data: LearningParameters): Unit

  def sendToCoordinator(out: Collector[OutMsg]): Unit
}

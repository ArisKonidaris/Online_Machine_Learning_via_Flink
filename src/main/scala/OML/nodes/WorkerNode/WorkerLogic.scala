package OML.nodes.WorkerNode

import OML.learners.Learner
import org.apache.flink.api.common.functions.FlatMapFunction
import org.apache.flink.streaming.api.checkpoint.CheckpointedFunction
import org.apache.flink.util.Collector

/** An abstract logic of a stateful worker node, implemented by a Flink FlatMapFunction
  *
  * @tparam InMsg  The input message type accepted by the worker
  * @tparam OutMsg The output message type the worker emits
  * @tparam MlAlgo The machine learning algorithm
  */
abstract class WorkerLogic[InMsg, OutMsg, MlAlgo <: Learner : Manifest]
  extends FlatMapFunction[InMsg, OutMsg] with CheckpointedFunction
    with Worker {
  override var learner: Learner = manifest[MlAlgo].erasure.newInstance.asInstanceOf[MlAlgo]

  def sendModelToServer(out: Collector[OutMsg]): Unit
}

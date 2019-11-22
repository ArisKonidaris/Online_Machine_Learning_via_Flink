package OML.nodes.WorkerNode

import OML.learners.Learner
import org.apache.flink.streaming.api.functions.co.RichCoFlatMapFunction
import org.apache.flink.util.Collector

/** An abstract logic of a stateful worker node, implemented by a Flink RichCoFlatMapFunction
  *
  * @tparam InMsg   The input message type accepted by the worker
  * @tparam CtrlMsg The control message send by the coordinator to te workers
  * @tparam OutMsg  The output message type the worker emits
  * @tparam MlAlgo  The machine learning algorithm
  */
abstract class RichCoWorkerLogic[InMsg, CtrlMsg, OutMsg, MlAlgo <: Learner : Manifest]
  extends RichCoFlatMapFunction[InMsg, CtrlMsg, OutMsg]
    with Worker {
  override var learner: Learner = manifest[MlAlgo].erasure.newInstance.asInstanceOf[MlAlgo]

  def sendModelToServer(out: Collector[OutMsg]): Unit
}

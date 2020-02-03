package OML.nodes.WorkerNode

import org.apache.flink.streaming.api.checkpoint.CheckpointedFunction
import org.apache.flink.streaming.api.functions.co.CoFlatMapFunction
import org.apache.flink.util.Collector

/** An abstract logic of a stateful worker node, implemented by a Flink CoFlatMapFunction
  *
  * @tparam InMsg   The input message type accepted by the worker
  * @tparam CtrlMsg The control message send by the coordinator to te workers
  * @tparam OutMsg  The output message type the worker emits
  */
abstract class CoWorkerLogic[InMsg, CtrlMsg, OutMsg]
  extends CoFlatMapFunction[InMsg, CtrlMsg, OutMsg]
    with CheckpointedFunction
    with Worker {
  def sendModelToServer(out: Collector[OutMsg]): Unit
}

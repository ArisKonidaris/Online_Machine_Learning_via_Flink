package OML.nodes.WorkerNode

import OML.learners.Learner
import org.apache.flink.streaming.api.checkpoint.CheckpointedFunction
import org.apache.flink.streaming.api.functions.co.CoFlatMapFunction

abstract class CoWorkerLogic[T, U, L <: Learner : Manifest]
  extends CoFlatMapFunction[T, T, U]
    with CheckpointedFunction
    with Worker[T, U] {
  override var learner: Learner = manifest[L].erasure.newInstance.asInstanceOf[L]
}

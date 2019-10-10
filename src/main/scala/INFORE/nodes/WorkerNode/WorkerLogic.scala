package INFORE.nodes.WorkerNode

import INFORE.learners.Learner
import org.apache.flink.api.common.functions.FlatMapFunction
import org.apache.flink.streaming.api.checkpoint.CheckpointedFunction

abstract class WorkerLogic[T, U, L <: Learner : Manifest]
  extends FlatMapFunction[T, U]
    with CheckpointedFunction
    with Worker[T, U] {
  override var learner: Learner = manifest[L].erasure.newInstance.asInstanceOf[L]
}

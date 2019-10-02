package INFORE.nodes.WorkerNode

import INFORE.learners.Learner
import org.apache.flink.api.common.functions.RichFlatMapFunction

abstract class SafeWorkerLogic[T, U, L <: Learner : Manifest]
  extends RichFlatMapFunction[T, U]
    with Worker[T, U] {
  //  override var learner: Learner = new L()
  override var learner: Learner = manifest[L].erasure.newInstance.asInstanceOf[L]
}

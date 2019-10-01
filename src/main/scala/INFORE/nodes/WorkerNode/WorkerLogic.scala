package INFORE.nodes.WorkerNode

import INFORE.learners.Learner
import org.apache.flink.api.common.functions.FlatMapFunction

abstract class WorkerLogic[T, U, L <: Learner]
  extends FlatMapFunction[T, U]
    with Worker[T, U] {
  override var learner: Learner = new L()
}

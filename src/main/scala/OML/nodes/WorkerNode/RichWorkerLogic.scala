package OML.nodes.WorkerNode

import OML.learners.Learner
import org.apache.flink.api.common.functions.RichFlatMapFunction

abstract class RichWorkerLogic[T, U, L <: Learner : Manifest]
  extends RichFlatMapFunction[T, U]
    with Worker[T, U] {
  override var learner: Learner = manifest[L].erasure.newInstance.asInstanceOf[L]
}

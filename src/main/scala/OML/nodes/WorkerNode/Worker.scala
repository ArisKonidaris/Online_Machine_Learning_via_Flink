package OML.nodes.WorkerNode

import OML.mlAPI.pipeline.Pipeline
import OML.parameters.LearningParameters
import org.apache.flink.api.common.functions.Function

import scala.collection.mutable.ListBuffer

/**
  * The basic trait for a Machine Learning worker node
  * A remote worker has an ML pipeline class that trains
  * on its local received stream
  *
  */
trait Worker extends Function with Serializable {
  var pipelines: scala.collection.mutable.Map[Int, Pipeline] = scala.collection.mutable.Map[Int, Pipeline]()

  def setWorkerId(workerID: Int): Unit

  def updatePipeline(pipelineID: Int, data: LearningParameters): Unit
}

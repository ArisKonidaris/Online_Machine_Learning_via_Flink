package OML.nodes.WorkerNode

import OML.parameters.LearningParameters
import OML.pipeline.Pipeline
import org.apache.flink.api.common.functions.Function

import scala.collection.mutable.ListBuffer

/**
  * The basic trait for a Machine Learning worker node
  * A remote worker has an ML pipeline class that trains
  * on its local received stream
  *
  */
trait Worker extends Function with Serializable {
  var pipelines: ListBuffer[Pipeline] = ListBuffer[Pipeline]()

  def setWorkerId(workerID: Int): Unit

  def updatePipeline(pipelineID: Int, data: LearningParameters): Unit
}

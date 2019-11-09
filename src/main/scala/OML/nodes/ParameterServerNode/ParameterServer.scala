package OML.nodes.ParameterServerNode

import OML.parameters.LearningParameters
import org.apache.flink.api.common.functions.Function

/**
  * The basic trait of a parameter server for distributed machine learning training
  */
trait ParameterServer extends Function with Serializable {
  def updateGlobalModel(localModel: LearningParameters): Unit
}
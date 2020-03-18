package oml.mlAPI.mlParameterServers.parameterServers

import oml.StarTopologyAPI.futures.{Response, ValueResponse}
import oml.StarTopologyAPI.sites.NetworkGraph
import oml.mlAPI.mlParameterServers.ParamServer
import oml.mlAPI.mlworkers.MLWorkerRemote
import oml.mlAPI.parameters.LearningParameters

class AsynchronousParameterServer extends MLParameterServer[MLWorkerRemote] with ParamServer {

  override def getInfo: Response[NetworkGraph] = ???

  override def pullModel: Response[LearningParameters] = {
    getGlobalLearnerParams match {
      case Some(mdl: LearningParameters) => new ValueResponse(mdl)
      case None => new ValueResponse(null)
    }
  }

  override def pushModel(model: LearningParameters): Response[LearningParameters] = ???
}

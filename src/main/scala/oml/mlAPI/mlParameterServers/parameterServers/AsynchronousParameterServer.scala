package oml.mlAPI.mlParameterServers.parameterServers

import oml.StarTopologyAPI.futures.{Response, ValueResponse}
import oml.mlAPI.mlParameterServers.PullPush
import oml.mlAPI.mlworkers.MLWorkerRemote
import oml.mlAPI.parameters.LearningParameters

class AsynchronousParameterServer extends MLParameterServer[MLWorkerRemote] with PullPush {

  override def getInfo: Response[NetworkGraph] = ???

  override def pullModel: Response[LearningParameters] = {
    getGlobalLearnerParams match {
      case Some(mdl: LearningParameters) => new ValueResponse(mdl)
      case None => new ValueResponse(null)
    }
  }

  override def pushModel(model: LearningParameters): Response[LearningParameters] = ???
}

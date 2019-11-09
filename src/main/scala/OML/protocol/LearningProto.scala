package OML.protocol

import OML.nodes.ParameterServerNode.{ParameterServer, RichPSLogic}
import OML.nodes.WorkerNode.Worker

/**
  * Base trait of a learning protocol
  */
trait LearningProto {
  def workerLogic: Worker

  def psLogic: ParameterServer
}

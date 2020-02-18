package oml.mlAPI.mlworkers

import oml.StarProtocolAPI.{RemoteOp, RemoteProxy}
import oml.math.Point
import oml.parameters.LearningParameters

import scala.collection.mutable.ListBuffer

@RemoteProxy
trait MLWorkerRemote {

  @RemoteOp(1)
  def updateModel(model: LearningParameters): Unit

  @RemoteOp(2)
  def score(testSet: ListBuffer[Point]): Unit

}

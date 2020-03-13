package oml.mlAPI.mlworkers

import oml.StarTopologyAPI.annotations.{RemoteOp, RemoteProxy}
import oml.math.Point
import oml.parameters.LearningParameters

import scala.collection.mutable.ListBuffer

@RemoteProxy
trait MLWorkerRemote {

  @RemoteOp
  def updateModel(model: LearningParameters): Unit

  @RemoteOp
  def score(testSet: ListBuffer[Point]): Unit

}

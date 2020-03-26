package oml.mlAPI.mlworkers

import oml.StarTopologyAPI.{RemoteOp, RemoteProxy}
import oml.math.Point
import oml.parameters.ParameterDescriptor

import scala.collection.mutable.ListBuffer

@RemoteProxy
trait MLWorkerRemote {

  @RemoteOp(1)
  def updateModel(model: ParameterDescriptor): Unit

  @RemoteOp(2)
  def score(testSet: ListBuffer[Point]): Unit

}

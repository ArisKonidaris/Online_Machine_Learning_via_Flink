package oml.FlinkBipartiteAPI.state

import breeze.linalg.{DenseVector => BreezeDenseVector}
import oml.mlAPI.math.Point
import oml.mlAPI.parameters.{LinearModelParameters, LearningParameters => lr_params}
import org.apache.flink.api.common.functions.AggregateFunction

import scala.collection.mutable
import scala.collection.mutable.ListBuffer
import java.io.Serializable

import oml.FlinkBipartiteAPI.POJOs.Request
import oml.FlinkBipartiteAPI.messages.{ControlMessage, SpokeMessage}
import oml.FlinkBipartiteAPI.network.FlinkNetwork
import oml.StarTopologyAPI.operations.RemoteCallIdentifier
import oml.StarTopologyAPI.sites.{NodeId, NodeType}
import oml.StarTopologyAPI.{BufferingWrapper, GenericWrapper}
import oml.mlAPI.dataBuffers.DataSet

class Counter(counter: Long) {
  def this() = this(0)
}

class ParameterAccumulator(params: lr_params) {
  def this() = this(LinearModelParameters(BreezeDenseVector.zeros[Double](1), 0.0))
}

class DataQueueAccumulator(dataSet: mutable.Queue[Point]) {
  def this() = this(mutable.Queue[Point]())
}

class DataListAccumulator(dataSet: ListBuffer[Point]) {
  def this() = this(ListBuffer[Point]())
}

class NodeAccumulator(node: GenericWrapper) {

  def this() = this(null)

  def getNodeWrapper: GenericWrapper = node
}

class NodeAggregateFunction()
  extends AggregateFunction[SpokeMessage, NodeAccumulator, GenericWrapper] {

  override def createAccumulator(): NodeAccumulator = new NodeAccumulator()

  override def add(message: SpokeMessage, acc: NodeAccumulator): NodeAccumulator = {
    acc.getNodeWrapper.receiveMsg(message.source, message.getOperation, Array[Any](message.getData))
    acc
  }

  override def getResult(acc: NodeAccumulator): GenericWrapper = acc.getNodeWrapper

  override def merge(acc: NodeAccumulator, acc1: NodeAccumulator): NodeAccumulator = {
    acc.getNodeWrapper.merge(Array(acc1.getNodeWrapper))
    acc
  }

}


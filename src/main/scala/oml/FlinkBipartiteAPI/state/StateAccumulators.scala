package oml.FlinkBipartiteAPI.state

import breeze.linalg.{DenseVector => BreezeDenseVector}
import oml.mlAPI.math.Point
import oml.mlAPI.parameters.{LinearModelParameters, LearningParameters => lr_params}
import org.apache.flink.api.common.functions.AggregateFunction

import scala.collection.mutable
import scala.collection.mutable.ListBuffer
import oml.FlinkBipartiteAPI.messages.{HubMessage, SpokeMessage}
import oml.FlinkBipartiteAPI.network.FlinkNetwork
import oml.StarTopologyAPI.GenericWrapper
import org.apache.flink.streaming.api.functions.KeyedProcessFunction
import org.apache.flink.util.Collector

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
  extends AggregateFunction[
    (SpokeMessage, KeyedProcessFunction[String, SpokeMessage, HubMessage]#Context, Collector[HubMessage]),
    NodeAccumulator, GenericWrapper
  ] {

  override def createAccumulator(): NodeAccumulator = new NodeAccumulator()

  override def add(message: (
    SpokeMessage,
      KeyedProcessFunction[String, SpokeMessage, HubMessage]#Context,
      Collector[HubMessage]
    ), acc: NodeAccumulator): NodeAccumulator = {

    message._1.getData match {
      case wrapper: GenericWrapper =>
        val newNodeAccumulator = new NodeAccumulator(wrapper)
        setCollectors(newNodeAccumulator, message._2, message._3)
        newNodeAccumulator
      case _ =>
        setCollectors(acc, message._2, message._3)
        acc.getNodeWrapper.receiveMsg(message._1.source, message._1.getOperation, message._1.getData)
        acc
    }

  }

  override def getResult(acc: NodeAccumulator): GenericWrapper = acc.getNodeWrapper

  override def merge(acc: NodeAccumulator, acc1: NodeAccumulator): NodeAccumulator = {
    acc.getNodeWrapper.merge(Array(acc1.getNodeWrapper))
    acc
  }

  private def setCollectors(a: NodeAccumulator,
                            ctx: KeyedProcessFunction[String, SpokeMessage, HubMessage]#Context,
                            out: Collector[HubMessage]): Unit = {
    val flinkNetwork = a.getNodeWrapper.getNetwork
      .asInstanceOf[FlinkNetwork[SpokeMessage, HubMessage, HubMessage]]
    flinkNetwork.setKeyedContext(ctx)
    flinkNetwork.setCollector(out)
  }


}


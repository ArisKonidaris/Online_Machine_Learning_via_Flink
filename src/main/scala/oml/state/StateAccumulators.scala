package oml.state

import breeze.linalg.{DenseVector => BreezeDenseVector}
import oml.math.Point
import oml.parameters.{LinearModelParameters, LearningParameters => lr_params}
import org.apache.flink.api.common.functions.AggregateFunction

import scala.collection.mutable
import scala.collection.mutable.ListBuffer

class Counter(val counter: Long) {
  def this() = this(0)
}

class DenseVectorAccumulator(var vector: BreezeDenseVector[Double]) {
  def this() = this(BreezeDenseVector.zeros[Double](1))

  def getVector: BreezeDenseVector[Double] = vector

  def setVector(vector: BreezeDenseVector[Double]): Unit = this.vector = vector

}

class ParameterAccumulator(val params: lr_params) {
  def this() = this(LinearModelParameters(BreezeDenseVector.zeros[Double](1), 0.0))
}

class DataQueueAccumulator(val dataSet: mutable.Queue[Point]) {
  def this() = this(mutable.Queue[Point]())
}

class DataListAccumulator(val dataSet: ListBuffer[Point]) {
  def this() = this(ListBuffer[Point]())
}

class LongAccumulator extends AggregateFunction[Long, Counter, Long] {

  def createAccumulator(): Counter = new Counter

  def merge(a: Counter, b: Counter): Counter = new Counter(a.counter + b.counter)

  def add(value: Long, acc: Counter): Counter = new Counter(acc.counter + value)

  def getResult(acc: Counter): Long = acc.counter
}

class AggregateDenseVectorFunction
  extends AggregateFunction[BreezeDenseVector[Double], DenseVectorAccumulator, BreezeDenseVector[Double]] {
  override def createAccumulator(): DenseVectorAccumulator = new DenseVectorAccumulator

  override def add(in: BreezeDenseVector[Double], acc: DenseVectorAccumulator): DenseVectorAccumulator = {
    try {
      acc.getVector += in
      acc
    } catch {
      case _: Throwable => new DenseVectorAccumulator(in)
    }
  }

  override def getResult(acc: DenseVectorAccumulator): BreezeDenseVector[Double] = acc.getVector

  override def merge(acc: DenseVectorAccumulator, acc1: DenseVectorAccumulator): DenseVectorAccumulator = {
    new DenseVectorAccumulator(acc.getVector + acc1.getVector)
  }
}

class modelAccumulator extends AggregateFunction[lr_params, ParameterAccumulator, lr_params] {

  def createAccumulator(): ParameterAccumulator = new ParameterAccumulator

  def merge(a: ParameterAccumulator, b: ParameterAccumulator): ParameterAccumulator = {
    new ParameterAccumulator(a.params + b.params)
  }

  def add(value: lr_params, acc: ParameterAccumulator): ParameterAccumulator = {
    try {
      require(value.getClass == acc.params.getClass)
      acc.params += value
      acc
    } catch {
      case _: Throwable => new ParameterAccumulator(value)
    }
  }

  def getResult(acc: ParameterAccumulator): lr_params = acc.params
}

class DataSetQueueAccumulator extends AggregateFunction[Point, DataQueueAccumulator, Option[Point]] {

  def createAccumulator(): DataQueueAccumulator = new DataQueueAccumulator

  def merge(a: DataQueueAccumulator, b: DataQueueAccumulator): DataQueueAccumulator = {
    new DataQueueAccumulator(
      {
        while (b.dataSet.nonEmpty) a.dataSet.enqueue(b.dataSet.dequeue)
        a.dataSet
      }
    )
  }

  def add(value: Point, acc: DataQueueAccumulator): DataQueueAccumulator = {
    acc.dataSet.enqueue(value)
    acc
  }

  def getResult(acc: DataQueueAccumulator): Option[Point] = {
    if (acc.dataSet.nonEmpty) Some(acc.dataSet.dequeue) else None
  }

}

class DataSetListAccumulator() extends AggregateFunction[Point, DataListAccumulator, Option[Point]] {

  def createAccumulator(): DataListAccumulator = new DataListAccumulator()

  def merge(a: DataListAccumulator, b: DataListAccumulator): DataListAccumulator = {
    new DataListAccumulator(
      {
        while (b.dataSet.nonEmpty) a.dataSet += b.dataSet.remove(0)
        a.dataSet
      }
    )
  }

  def add(value: Point, acc: DataListAccumulator): DataListAccumulator = {
    acc.dataSet += value
    acc
  }

  def getResult(acc: DataListAccumulator): Option[Point] = {
    if (acc.dataSet.nonEmpty) {
      val data = acc.dataSet.head
      acc.dataSet.remove(0)
      Some(data)
    } else None
  }

}

package OML.common

import OML.parameters.{LinearModelParameters, LearningParameters => lr_params}
import breeze.linalg.{DenseVector => BreezeDenseVector}
import org.apache.flink.api.common.functions.AggregateFunction

import scala.collection.mutable

class Counter(val counter: Int) {
  def this() = this(0)
}

class ParameterAccumulator(val params: lr_params) {
  def this() = this(LinearModelParameters(BreezeDenseVector.zeros[Double](1), 0.0))
}

class DataQueueAccumulator(val train_set: mutable.Queue[Point]) {
  def this() = this(mutable.Queue[Point]())
}

class IntegerAccumulator extends AggregateFunction[Int, Counter, Int] {

  def createAccumulator(): Counter = new Counter

  def merge(a: Counter, b: Counter): Counter = new Counter(a.counter + b.counter)

  def add(value: Int, acc: Counter): Counter = new Counter(acc.counter + value)

  def getResult(acc: Counter): Int = acc.counter
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

class DataSetAccumulator extends AggregateFunction[Point, DataQueueAccumulator, Option[Point]] {

  def createAccumulator(): DataQueueAccumulator = new DataQueueAccumulator

  def merge(a: DataQueueAccumulator, b: DataQueueAccumulator): DataQueueAccumulator = {
    new DataQueueAccumulator(
      {
        while (b.train_set.nonEmpty) a.train_set.enqueue(b.train_set.dequeue)
        a.train_set
      }
    )
  }

  def add(value: Point, acc: DataQueueAccumulator): DataQueueAccumulator = {
    acc.train_set.enqueue(value)
    acc
  }

  def getResult(acc: DataQueueAccumulator): Option[Point] = {
    if (acc.train_set.nonEmpty) Some(acc.train_set.dequeue) else None
  }

}

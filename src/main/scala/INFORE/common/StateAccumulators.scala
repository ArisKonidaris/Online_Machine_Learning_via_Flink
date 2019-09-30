package INFORE.common

import INFORE.parameters.{LinearModelParameters, LearningParameters => lr_params}
import breeze.linalg.{DenseVector => BreezeDenseVector}
import org.apache.flink.api.common.functions.AggregateFunction

class Counter(val counter: Int) {
  def this() = this(0)
}

class ParameterAccumulator(val params: lr_params) {
  def this() = this(LinearModelParameters(BreezeDenseVector.zeros[Double](1), 0.0))
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
      new ParameterAccumulator(acc.params + value)
    } catch {
      case _: Throwable => new ParameterAccumulator(value)
    }
  }

  def getResult(acc: ParameterAccumulator): lr_params = acc.params
}

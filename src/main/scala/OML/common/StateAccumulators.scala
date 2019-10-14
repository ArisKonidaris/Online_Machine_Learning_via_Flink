package OML.common

import OML.parameters.{LinearModelParameters, LearningParameters => l_params}
import breeze.linalg.{DenseVector => BreezeDenseVector}
import org.apache.flink.api.common.functions.AggregateFunction

class Counter(val counter: Int) {
  def this() = this(0)
}

class ParameterAccumulator(val params: l_params) {
  def this() = this(LinearModelParameters(BreezeDenseVector.zeros[Double](1), 0.0))
}

class IntegerAccumulator extends AggregateFunction[Int, Counter, Int] {

  def createAccumulator(): Counter = new Counter

  def merge(a: Counter, b: Counter): Counter = new Counter(a.counter + b.counter)

  def add(value: Int, acc: Counter): Counter = new Counter(acc.counter + value)

  def getResult(acc: Counter): Int = acc.counter
}

class modelAccumulator extends AggregateFunction[l_params, ParameterAccumulator, l_params] {

  def createAccumulator(): ParameterAccumulator = new ParameterAccumulator

  def merge(a: ParameterAccumulator, b: ParameterAccumulator): ParameterAccumulator = {
    new ParameterAccumulator(a.params + b.params)
  }

  def add(value: l_params, acc: ParameterAccumulator): ParameterAccumulator = {
    try {
      require(value.getClass == acc.params.getClass)
      //      new ParameterAccumulator(acc.params + value)
      acc.params += value
      acc
    } catch {
      case _: Throwable => new ParameterAccumulator(value)
    }
  }

  def getResult(acc: ParameterAccumulator): l_params = acc.params
}

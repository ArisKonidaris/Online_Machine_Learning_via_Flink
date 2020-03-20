package oml.mlAPI.parameters

import breeze.linalg.{DenseVector => BreezeDenseVector}
import oml.math.{DenseVector, SparseVector, Vector}

import scala.collection.mutable.ListBuffer

/**
  * A trait for determining if the Learning Parameters are
  * represented by the Breeze library.
  */
trait BreezeParameters extends LearningParameters {

  def flatten: BreezeDenseVector[Double]

  def unwrapData(sizes: Array[Int], data: Array[Double]): ListBuffer[Array[Double]] = {
    require(sizes.sum == data.length)

    @scala.annotation.tailrec
    def recursiveUnwrapping(sz: Array[Int], dt: Array[Double], result: ListBuffer[Array[Double]])
    : ListBuffer[Array[Double]] = {
      if (sz.isEmpty) {
        result
      } else {
        result.append(dt.slice(0, sz.head))
        recursiveUnwrapping(sz.tail, dt.slice(sz.head, dt.length), result)
      }
    }

    recursiveUnwrapping(sizes, data, new ListBuffer[Array[Double]])
  }

  override def slice(range: Range, sparse: Boolean): Vector = {
    sliceRequirements(range)
    if (sparse)
      SparseVector.sparseVectorConverter.convert(flatten(range.getStart to range.getEnd))
    else
      DenseVector.denseVectorConverter.convert(flatten(range.getStart to range.getEnd))
  }

}

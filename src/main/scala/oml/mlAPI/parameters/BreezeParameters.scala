package oml.mlAPI.parameters

import breeze.linalg.{DenseVector => BreezeDenseVector, SparseVector => BreezeSparseVector}
import oml.math.{DenseVector, SparseVector, Vector}

/**
  * A trait for determining if the Learning Parameters are
  * represented by the Breeze library.
  */
trait BreezeParameters extends LearningParameters {

  def flatten: BreezeDenseVector[Double]

  override def slice(indexRanges: (Int, Int), sparse: Boolean): Vector = {
    sliceRequirements(indexRanges)
    if (sparse)
      SparseVector.sparseVectorConverter.convert(flatten(indexRanges._1 to indexRanges._2))
    else
      DenseVector.denseVectorConverter.convert(flatten(indexRanges._1 to indexRanges._2))
  }

}

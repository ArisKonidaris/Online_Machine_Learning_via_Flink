package oml.mlAPI.math

import breeze.linalg.{CSCMatrix => BreezeCSCMatrix, DenseMatrix => BreezeDenseMatrix, DenseVector => BreezeDenseVector, Matrix => BreezeMatrix, SparseVector => BreezeSparseVector, Vector => BreezeVector}

/** This class contains convenience function to wrap a matrix/vector into a breeze matrix/vector
  * and to unwrap it again.
  *
  */
object Breeze {

  implicit class Matrix2BreezeConverter(matrix: Matrix) {
    def asBreeze: BreezeMatrix[Double] = {
      matrix match {
        case dense: DenseMatrix =>
          new BreezeDenseMatrix[Double](
            dense.numRows,
            dense.numCols,
            dense.data)

        case sparse: SparseMatrix =>
          new BreezeCSCMatrix[Double](
            sparse.data,
            sparse.numRows,
            sparse.numCols,
            sparse.colPtrs,
            sparse.rowIndices
          )
      }
    }
  }

  implicit class Breeze2MatrixConverter(matrix: BreezeMatrix[Double]) {
    def fromBreeze: Matrix = {
      matrix match {
        case dense: BreezeDenseMatrix[Double] =>
          new DenseMatrix(dense.rows, dense.cols, dense.data)

        case sparse: BreezeCSCMatrix[Double] =>
          new SparseMatrix(sparse.rows, sparse.cols, sparse.rowIndices, sparse.colPtrs, sparse.data)
      }
    }
  }

  implicit class BreezeArrayConverter[T](array: Array[T]) {
    def asBreeze: BreezeDenseVector[T] = {
      new BreezeDenseVector[T](array)
    }
  }

  implicit class Breeze2VectorConverter(vector: BreezeVector[Double]) {
    def fromBreeze[T <: Vector : BreezeVectorConverter]: T = {
      val converter = implicitly[BreezeVectorConverter[T]]
      converter.convert(vector)
    }
  }

  implicit class Vector2BreezeConverter(vector: Vector) {
    def asBreeze: BreezeVector[Double] = {
      vector match {
        case dense: DenseVector =>
          new breeze.linalg.DenseVector(dense.data)

        case sparse: SparseVector =>
          new BreezeSparseVector(sparse.indices, sparse.data, sparse.size)
      }
    }
  }

}

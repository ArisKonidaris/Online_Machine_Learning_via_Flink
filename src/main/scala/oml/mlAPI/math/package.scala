package oml

import oml.mlAPI.math.{DenseVector, SparseVector}

/**
  * Convenience methods to handle Flink's [[oml.mlAPI.math.Matrix]] and [[Vector]]
  * abstraction.
  */
package object math {

  implicit class RichMatrix(matrix: oml.mlAPI.math.Matrix) extends Iterable[(Int, Int, Double)] {

    override def iterator: Iterator[(Int, Int, Double)] = {
      new Iterator[(Int, Int, Double)] {
        var index = 0

        override def hasNext: Boolean = {
          index < matrix.numRows * matrix.numCols
        }

        override def next(): (Int, Int, Double) = {
          val row = index % matrix.numRows
          val column = index / matrix.numRows

          index += 1

          (row, column, matrix(row, column))
        }
      }
    }

    def valueIterator: Iterator[Double] = {
      val it = iterator

      new Iterator[Double] {
        override def hasNext: Boolean = it.hasNext

        override def next(): Double = it.next._3
      }
    }

  }

  implicit class RichVector(vector: oml.mlAPI.math.Vector) extends Iterable[(Int, Double)] {

    override def iterator: Iterator[(Int, Double)] = {
      new Iterator[(Int, Double)] {
        var index = 0

        override def hasNext: Boolean = {
          index < vector.size
        }

        override def next(): (Int, Double) = {
          val resultIndex = index

          index += 1

          (resultIndex, vector(resultIndex))
        }
      }
    }

    def valueIterator: Iterator[Double] = {
      val it = iterator

      new Iterator[Double] {
        override def hasNext: Boolean = it.hasNext

        override def next(): Double = it.next._2
      }
    }
  }

  /** Stores the vector values request a dense array
    *
    * @param vector Subtype of [[Vector]]
    * @return Array containing the vector values
    */
  def vector2Array(vector: oml.mlAPI.math.Vector): Array[Double] = {
    vector match {
      case dense: DenseVector => dense.data.clone

      case sparse: SparseVector => {
        val result = new Array[Double](sparse.size)

        for ((index, value) <- sparse) {
          result(index) = value
        }

        result
      }
    }
  }
}

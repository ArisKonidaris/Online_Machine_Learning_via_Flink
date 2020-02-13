package oml.math

import breeze.linalg.{DenseVector => BreezeDenseVector, SparseVector => BreezeSparseVector, Vector => BreezeVector}

/** Base trait for Vectors
  *
  */
trait Vector extends Serializable {

  /** Number of elements in a vector
    *
    * @return The number of elements of the vector
    */
  def size: Int

  /** Element wise access function
    *
    * @param index index of the accessed element
    * @return value of the associated with the index
    */
  def apply(index: Int): Double

  /** Updates the element at the given index with the provided value
    *
    * @param index The index of the element to be updated
    * @param value The new value
    */
  def update(index: Int, value: Double): Unit

  /** Copies the vector instance
    *
    * @return Copy of the vector instance
    */
  def copy: Vector

  /** Returns the dot product of the recipient and the argument
    *
    * @param other a Vector
    * @return a scalar double of dot product
    */
  def dot(other: Vector): Double

  /** Returns the outer product of the recipient and the argument
    *
    * @param other a Vector
    * @return a matrix
    */
  def outer(other: Vector): Matrix

  /** Magnitude of a vector
    *
    * @return The length of the vector
    */
  def magnitude: Double

  def equalsVector(vector: Vector): Boolean = {
    if (size == vector.size) {
      (0 until size) forall { idx =>
        this (idx) == vector(idx)
      }
    } else {
      false
    }
  }

  /** Converts the vector to a List of Doubles
    *
    * @return A List of Doubles
    */
  def toList: List[Double]

}

object Vector {
  /** BreezeVectorConverter implementation for [[Vector]]
    *
    * This allows to convert Breeze vectors into [[Vector]].
    */
  implicit val vectorConverter = new BreezeVectorConverter[Vector] {
    override def convert(vector: BreezeVector[Double]): Vector = {
      vector match {
        case dense: BreezeDenseVector[Double] => new DenseVector(dense.data)

        case sparse: BreezeSparseVector[Double] =>
          new SparseVector(
            sparse.length,
            sparse.index.take(sparse.used),
            sparse.data.take(sparse.used))
      }
    }
  }
}


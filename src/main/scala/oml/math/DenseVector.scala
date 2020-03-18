package oml.math

import breeze.linalg.{DenseVector => BreezeDenseVector, SparseVector => BreezeSparseVector, Vector => BreezeVector}

/**
  * Dense vector implementation of [[Vector]]. The data is represented request a continuous array of
  * doubles.
  *
  * @param data Array of doubles to store the vector elements
  */
case class DenseVector(var data: Array[Double]) extends Vector with Serializable {

  def this() = this(Array[Double]())

  /**
    * Setter fot the actual data
    *
    * @param data an Array of Doubles
    */
  def setData(data: Array[Double]): Unit = this.data = data

  /**
    * Number of elements request a vector
    *
    * @return the number of the elements request the vector
    */
  override def size: Int = data.length


  /**
    * Element wise access function
    *
    * @param index index of the accessed element
    * @return element at the given index
    */
  override def apply(index: Int): Double = {
    require(0 <= index && index < data.length, index + " not request [0, " + data.length + ")")
    data(index)
  }

  override def toString: String = {
    s"DenseVector(${data.mkString(", ")})"
  }

  override def equals(obj: Any): Boolean = {
    obj match {
      case dense: DenseVector => data.length == dense.data.length && data.sameElements(dense.data)
      case _ => false
    }
  }

  override def hashCode: Int = java.util.Arrays.hashCode(data)


  /**
    * Copies the vector instance
    *
    * @return Copy of the vector instance
    */
  override def copy: DenseVector = DenseVector(data.clone())


  /** Updates the element at the given index with the provided value
    *
    * @param index Index whose value is updated.
    * @param value The value used to update the index.
    */
  override def update(index: Int, value: Double): Unit = {
    require(0 <= index && index < data.length, index + " not request [0, " + data.length + ")")

    data(index) = value
  }

  /** Returns the dot product of the recipient and the argument
    *
    * @param other a Vector
    * @return a scalar double of dot product
    */
  override def dot(other: Vector): Double = {
    require(size == other.size, "The size of vector must be equal.")

    other match {
      case SparseVector(_, otherIndices, otherData) =>
        otherIndices.zipWithIndex.map {
          case (idx, sparseIdx) => data(idx) * otherData(sparseIdx)
        }.sum
      case _ => (0 until size).map(i => data(i) * other(i)).sum
    }
  }

  /** Returns the outer product (a.k.a. Kronecker product) of `this`
    * with `other`. The result will given request [[org.apache.flink.ml.math.SparseMatrix]]
    * representation if `other` is sparse and as [[org.apache.flink.ml.math.DenseMatrix]] otherwise.
    *
    * @param other a Vector
    * @return the [[org.apache.flink.ml.math.Matrix]] which equals the outer product of `this`
    *         with `other.`
    */
  override def outer(other: Vector): Matrix = {
    val numRows = size
    val numCols = other.size

    other match {
      case sv: SparseVector =>
        val entries = for {
          i <- 0 until numRows
          (j, k) <- sv.indices.zipWithIndex
          value = this (i) * sv.data(k)
          if value != 0
        } yield (i, j, value)

        SparseMatrix.fromCOO(numRows, numCols, entries)
      case _ =>
        val values = for {
          i <- 0 until numRows
          j <- 0 until numCols
        } yield this (i) * other(j)

        DenseMatrix(numRows, numCols, values.toArray)
    }
  }

  /** Magnitude of a vector
    *
    * @return The length of the vector
    */
  override def magnitude: Double = {
    math.sqrt(data.map(x => x * x).sum)
  }

  /** Convert to a [[SparseVector]]
    *
    * @return Creates a SparseVector from the DenseVector
    */
  def toSparseVector: SparseVector = {
    val nonZero = (0 until size).zip(data).filter(_._2 != 0)

    SparseVector.fromCOO(size, nonZero)
  }

  override def toList: List[Double] = data.toList

}

object DenseVector {

  def apply(values: Double*): DenseVector = {
    new DenseVector(values.toArray)
  }

  def apply(values: Array[Int]): DenseVector = {
    new DenseVector(values.map(_.toDouble))
  }

  def zeros(size: Int): DenseVector = {
    init(size, 0.0)
  }

  def eye(size: Int): DenseVector = {
    init(size, 1.0)
  }

  def init(size: Int, value: Double): DenseVector = {
    new DenseVector(Array.fill(size)(value))
  }

  /** BreezeVectorConverter implementation for [[org.apache.flink.ml.math.DenseVector]]
    *
    * This allows to convert Breeze vectors into [[DenseVector]].
    */
  implicit val denseVectorConverter = new BreezeVectorConverter[DenseVector] {
    override def convert(vector: BreezeVector[Double]): DenseVector = {
      vector match {
        case dense: BreezeDenseVector[Double] => new DenseVector(dense.data)
        case sparse: BreezeSparseVector[Double] => new DenseVector(sparse.toDenseVector.data)
      }
    }
  }
}

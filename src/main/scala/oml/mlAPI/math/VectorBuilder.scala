package oml.mlAPI.math

/** Type class to allow the vector construction from different data types
  *
  * @tparam T Subtype of [[Vector]]
  */
trait VectorBuilder[T <: Vector] extends Serializable {
  /** Builds a [[Vector]] of type T from a List[Double]
    *
    * @param data Input data where the index denotes the resulting index of the vector
    * @return A vector of type T
    */
  def build(data: List[Double]): T
}

object VectorBuilder {

  /** Type class implementation for [[org.apache.flink.ml.math.DenseVector]] */
  implicit val denseVectorBuilder = new VectorBuilder[DenseVector] {
    override def build(data: List[Double]): DenseVector = {
      new DenseVector(data.toArray)
    }
  }

  /** Type class implementation for [[org.apache.flink.ml.math.SparseVector]] */
  implicit val sparseVectorBuilder = new VectorBuilder[SparseVector] {
    override def build(data: List[Double]): SparseVector = {
      // Enrich elements with explicit indices and filter out zero entries
      SparseVector.fromCOO(data.length, data.indices.zip(data).filter(_._2 != 0.0))
    }
  }

  /** Type class implementation for [[Vector]] */
  implicit val vectorBuilder = new VectorBuilder[Vector] {
    override def build(data: List[Double]): Vector = {
      new DenseVector(data.toArray)
    }
  }
}

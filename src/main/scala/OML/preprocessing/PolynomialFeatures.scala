package OML.preprocessing

import OML.common.{Parameter, ParameterMap, WithParameters}
import OML.math.{DenseVector, LabeledPoint, Point, UnlabeledPoint}
import OML.preprocessing.PolynomialFeatures.{Degree, polynomial}

/** Maps a vector into the polynomial feature space.
  *
  * This preprocessor takes a a vector of values `(x, y, z, ...)` and maps it into the
  * polynomial feature space of degree `d`. That is to say, it calculates the following
  * representation:
  *
  * `(x, y, z, x^2, xy, y^2, yz, z^2, x^3, x^2y, x^2z, xyz, ...)^T`
  *
  * =Parameters=
  *
  *  - [[OML.preprocessing.PolynomialFeatures.Degree]]: Maximum polynomial degree
  */
case class PolynomialFeatures() extends preprocessing with WithParameters {

  override def transform(data: Point): Point = polynomial(data, parameters(Degree))

  def setDegree(degree: Int): PolynomialFeatures = {
    parameters.add(Degree, degree)
    this
  }

}

object PolynomialFeatures {

  // ====================================== Parameters =============================================

  case object Degree extends Parameter[Int] {
    override val defaultValue: Option[Int] = Some(2)
  }

  // =================================== Factory methods ===========================================

  def apply(): PolynomialFeatures = {
    new PolynomialFeatures()
  }

  // ====================================== Operations =============================================

  def polynomial(data: Point, degree: Int): Point = {
    data match {
      case LabeledPoint(label, vector) =>
        LabeledPoint(label, DenseVector(combinations(vector.toList, degree).toArray))
      case UnlabeledPoint(vector) => UnlabeledPoint(DenseVector(combinations(vector.toList, degree).toArray))
    }
  }

  def combinations(features: List[Double], degree: Int): List[Double] = {
    @scala.annotation.tailrec
    def af0(acc: List[Double], p: Int): List[Double] =
      if (p == 0) acc else af0(acc ++ (for {
        s1 <- acc
        s2 <- features
      } yield s1 * s2), p - 1)

    af0(features, degree - 1)
  }

}

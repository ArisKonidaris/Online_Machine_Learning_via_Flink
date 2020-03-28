package oml.mlAPI.preprocessing

import oml.mlAPI.math.{DenseVector, LabeledPoint, Point, UnlabeledPoint}

import scala.collection.mutable
import scala.collection.JavaConverters._
import scala.collection.mutable.ListBuffer

/** Maps a vector into the polynomial feature space.
  *
  * This preprocessor takes a a vector of values `(x, y, z, ...)` and maps it into the
  * polynomial feature space of degree `d`. That is to say, it calculates the following
  * representation:
  *
  * `(x, y, z, x^2, xy, y^2, yz, z^2, x^3, x^2y, x^2z, xyz, ...)^T`
  *
  */
case class PolynomialFeatures() extends Preprocessor {

  import PolynomialFeatures._

  private var degree: Int = 2

  override def transform(point: Point): Point = polynomial(point, degree)

  override def transform(dataSet: ListBuffer[Point]): ListBuffer[Point] = {
    val transformedSet = ListBuffer[Point]()
    for (data <- dataSet) transformedSet.append(transform(data))
    transformedSet
  }

  def setDegree(degree: Int): PolynomialFeatures = {
    this.degree = degree
    this
  }

  override def setHyperParametersFromMap(hyperParameterMap: mutable.Map[String, AnyRef]): Preprocessor = {
    for ((hyperparameter, value) <- hyperParameterMap) {
      hyperparameter match {
        case "degree" =>
          try {
            setDegree(value.asInstanceOf[Int])
          } catch {
            case e: Exception =>
              println("Error while trying to update the degree of Polynomial Features")
              e.printStackTrace()

            case _: Throwable =>
          }
      }
    }
    this
  }

  override def generatePOJOPreprocessor: oml.FlinkBipartiteAPI.POJOs.Preprocessor = {
    new oml.FlinkBipartiteAPI.POJOs.Preprocessor("PolynomialFeatures",
      Map[String, AnyRef](("degree", degree.asInstanceOf[AnyRef])).asJava,
      null
    )
  }

}

object PolynomialFeatures {


  // =================================== Factory methods ===========================================

  def apply(): PolynomialFeatures = {
    new PolynomialFeatures()
  }

  // ====================================== Operations =============================================

  def polynomial(point: Point, degree: Int): Point = {
    point match {
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

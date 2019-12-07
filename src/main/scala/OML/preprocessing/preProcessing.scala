package OML.preprocessing

import OML.math.Point

import scala.collection.mutable.ListBuffer

trait preProcessing extends Serializable {
  def transform(point: Point): Point

  def transform(dataSet: ListBuffer[Point]): ListBuffer[Point]
}

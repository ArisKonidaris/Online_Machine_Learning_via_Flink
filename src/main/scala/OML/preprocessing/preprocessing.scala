package OML.preprocessing

import OML.math.Point

trait preprocessing extends Serializable {
  def transform(vector: Point): Point
}

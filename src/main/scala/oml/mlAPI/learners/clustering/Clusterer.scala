package oml.mlAPI.learners.clustering

import oml.mlAPI.learners.Learner
import oml.mlAPI.math.Point

trait Clusterer extends Learner {

  def distribution(data: Point): Array[Double]

}

package oml.mlAPI.scores

trait Score extends Serializable {
  def getScore: Double
}

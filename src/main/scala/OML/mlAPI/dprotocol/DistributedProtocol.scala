package OML.mlAPI.dprotocol

trait DistributedProtocol extends Serializable {
  def sendToPS(): Boolean
}

object DistributedProtocol {
  val protocols: List[String] = List("Asynchronous", "Synchronous", "DynamicAveraging", "FGM")
}
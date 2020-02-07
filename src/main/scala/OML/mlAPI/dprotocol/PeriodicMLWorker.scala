package OML.mlAPI.dprotocol

/**
  * A machine learning worker that periodically sends messages to the coordinator.
  * This machine learning worker is used in asynchronous and synchronous
  * distributed training paradigm.
  */
case class PeriodicMLWorker() extends DistributedProtocol {
  override def sendToPS(): Boolean = true
}

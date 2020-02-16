package oml.mlAPI.mlworkers

import java.io

import oml.StarProtocolAPI.WorkerGenerator
import oml.message.packages.MLWorkerConfig
import oml.mlAPI.mlworkers.worker.PeriodicMLWorker
import oml.mlAPI.types.protocols._

case class MLWorkerGenerator() extends WorkerGenerator {
  override def generate(config: io.Serializable): AnyRef = {
    try {
      val conf: MLWorkerConfig = config.asInstanceOf[MLWorkerConfig]
      conf.proto match {
        case AsynchronousAveraging => PeriodicMLWorker().configureWorker(conf)
        case SynchronousAveraging => PeriodicMLWorker().configureWorker(conf)
        case DynamicAveraging => PeriodicMLWorker().configureWorker(conf)
        case FGMAveraging => PeriodicMLWorker().configureWorker(conf)
        case _ => PeriodicMLWorker().configureWorker(conf)
      }
    } catch {
      case e: Exception => throw new RuntimeException("Something went wrong while creating a new ML Worker", e)
    }
  }
}

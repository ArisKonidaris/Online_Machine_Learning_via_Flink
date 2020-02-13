package oml.mlAPI.mlworkers

import java.io

import oml.StarProtocolAPI.{GenericWrapper, Node, NodeGenerator}
import oml.message.packages.MLWorkerConfig
import oml.mlAPI.mlworkers.worker.PeriodicMLWorker
import oml.mlAPI.types.protocols._

case class MLWorkerGenerator() extends NodeGenerator {
  override def generate(config: io.Serializable): Node = {
    val conf: MLWorkerConfig = config.asInstanceOf[MLWorkerConfig]
    conf.proto match {
      case AsynchronousAveraging => new GenericWrapper(PeriodicMLWorker().configureWorker(conf))
      case SynchronousAveraging => new GenericWrapper(PeriodicMLWorker().configureWorker(conf))
      case DynamicAveraging => new GenericWrapper(PeriodicMLWorker().configureWorker(conf))
      case FGMAveraging => new GenericWrapper(PeriodicMLWorker().configureWorker(conf))
      case _ => new GenericWrapper(PeriodicMLWorker().configureWorker(conf))
    }
  }
}

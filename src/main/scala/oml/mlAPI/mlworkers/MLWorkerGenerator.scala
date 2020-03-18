package oml.mlAPI.mlworkers

import oml.FlinkAPI.POJOs.Request
import oml.StarTopologyAPI.NodeGenerator
import oml.mlAPI.mlworkers.worker.PeriodicMLWorker

import scala.collection.mutable
import scala.collection.JavaConverters._

case class MLWorkerGenerator() extends NodeGenerator {
  override def generate(request: Request): AnyRef = {
    try {
      val config: mutable.Map[String, AnyRef] = request.getTraining_configuration.asScala
      if (config.contains("protocol"))
        try {
          config.get("protocol").asInstanceOf[String] match {
            case "Asynchronous" => PeriodicMLWorker().configureWorker(request)
            case "Synchronous" => PeriodicMLWorker().configureWorker(request)
            case "DynamicAveraging" => PeriodicMLWorker().configureWorker(request)
            case "FGMAveraging" => PeriodicMLWorker().configureWorker(request)
            case _ => PeriodicMLWorker().configureWorker(request)
          }
        } catch {
            case _: Throwable => PeriodicMLWorker().configureWorker(request)
        }
      else PeriodicMLWorker().configureWorker(request)
    } catch {
      case e: Exception => throw new RuntimeException("Something went wrong while creating a new ML Worker", e)
    }
  }
}

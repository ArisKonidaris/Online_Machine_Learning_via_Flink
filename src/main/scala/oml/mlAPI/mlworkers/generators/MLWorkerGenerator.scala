package oml.mlAPI.mlworkers.generators

import oml.FlinkBipartiteAPI.POJOs.Request
import oml.StarTopologyAPI.NodeGenerator
import oml.mlAPI.mlworkers.worker.MLPeriodicWorker

import scala.collection.mutable

case class MLWorkerGenerator() extends NodeGenerator {
  override def generate(request: Request): AnyRef = {
    try {
      val config: mutable.Map[String, AnyRef] = request.getTraining_configuration.asScala
      if (config.contains("protocol"))
        try {
          config.get("protocol").asInstanceOf[String] match {
            case "Asynchronous" => MLPeriodicWorker().configureWorker(request)
            case "Synchronous" => MLPeriodicWorker().configureWorker(request)
            case "DynamicAveraging" => MLPeriodicWorker().configureWorker(request)
            case "FGMAveraging" => MLPeriodicWorker().configureWorker(request)
            case _ => MLPeriodicWorker().configureWorker(request)
          }
        } catch {
            case _: Throwable => MLPeriodicWorker().configureWorker(request)
        }
      else MLPeriodicWorker().configureWorker(request)
    } catch {
      case e: Exception => throw new RuntimeException("Something went wrong while creating a new ML Worker", e)
    }
  }
}

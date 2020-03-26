package oml.mlAPI.mlworkers

import oml.POJOs.Request
import oml.StarTopologyAPI.WorkerGenerator
import oml.mlAPI.mlworkers.worker.{MLPeriodicWorker, MLPredictor}

import scala.collection.mutable
import scala.collection.JavaConverters._

case class MLNodeGenerator() extends WorkerGenerator {
  override def generateTrainingWorker(request: Request): AnyRef = {
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
      case e: Exception => throw new RuntimeException("Something went wrong while creating an ML Training Worker", e)
    }
  }

  override def generatePredictionWorker(request: Request): AnyRef = {
    try {
      new MLPredictor().configureWorker(request)
    } catch {
      case e: Exception => throw new RuntimeException("Something went wrong while creating an ML Predictor Worker", e)
    }
  }
}

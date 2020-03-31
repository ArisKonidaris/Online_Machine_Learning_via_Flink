package oml.mlAPI.mlworkers.generators

import oml.FlinkBipartiteAPI.POJOs.Request
import oml.StarTopologyAPI.{NodeGenerator, NodeInstance}
import oml.mlAPI.mlParameterServers.parameterServers.AsynchronousParameterServer
import oml.mlAPI.mlworkers.worker.MLPeriodicWorker

import scala.collection.mutable
import scala.collection.JavaConverters._

case class MLNodeGenerator() extends NodeGenerator {

  override def generateSpokeNode(request: Request): NodeInstance[_, _] = {
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
      case e: Exception => throw new RuntimeException("Something went wrong while creating a new ML FlinkSpoke", e)
    }
  }

  override def generateHubNode(request: Request): NodeInstance[_, _] = {
    try {
      val config: mutable.Map[String, AnyRef] = request.getTraining_configuration.asScala
      if (config.contains("protocol"))
        try {
          config.get("protocol").asInstanceOf[String] match {
            case "Asynchronous" => AsynchronousParameterServer().configureParameterServer(request)
            case "Synchronous" => AsynchronousParameterServer().configureParameterServer(request)
            case "DynamicAveraging" => AsynchronousParameterServer().configureParameterServer(request)
            case "FGMAveraging" => AsynchronousParameterServer().configureParameterServer(request)
            case _ => AsynchronousParameterServer().configureParameterServer(request)
          }
        } catch {
          case _: Throwable => AsynchronousParameterServer().configureParameterServer(request)
        }
      else AsynchronousParameterServer().configureParameterServer(request)
    } catch {
      case e: Exception => throw new RuntimeException("Something went wrong while creating a new ML FlinkHub", e)
    }
  }
}

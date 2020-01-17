package OML.utils.parsers

import OML.message.ControlMessage
import OML.message.packages._
import OML.message.packages.TransformerContainer
import org.apache.flink.api.common.functions.FlatMapFunction
import org.apache.flink.util.Collector

import scala.util.parsing.json.JSON

class RequestParser() extends FlatMapFunction[String, ControlMessage] {

  override def flatMap(in: String, collector: Collector[ControlMessage]): Unit = {
    val parse = JSON.parseFull(in.stripMargin)
    parse match {
      case Some(map: Map[String, Any]) =>
        try {
          if (checkValidity(map)) {
            if (checkRequestValidity(map)) parsePipeline(map, collector)
          } else println("Parsing failed. A request must contain the keys \"id\" and \"request\".")
        } catch {
          case e: Exception =>
            println("Parsing failed")
            e.printStackTrace()
        }
      case None => println("Parsing failed")
      case other => println("Unknown data structure: " + other)
    }
  }

  private def checkValidity(map: Map[String, Any]): Boolean = map.contains("id") && map.contains("request")

  private def checkRequestValidity(map: Map[String, Any]): Boolean = {
    map("request") match {
      case "Create" => true
      case "Update" => true
      case "Delete" => true
      case _ =>
        println("Parsing failed. A request must contain one of the values " +
          "\"Create\", \"Update\", \"Delete\" for the key \"request\"")
        false
    }
  }

  private def checkNameValidity(transformer: String, name: String): Boolean = {
    transformer match {
      case "preprocessors" =>
        name match {
          case "PolynomialFeatures" => true
          case "StandardScaler" => true
          case _ => false
        }
      case "learner" =>
        name match {
          case "PA" => true
          case "regressorPA" => true
          case "ORR" => true
          case _ => false
        }
      case _ => false
    }
  }

  private def parsePipeline(map: Map[String, Any], collector: Collector[ControlMessage]): Unit = {
    val pipelineId: Int = map("id").asInstanceOf[Double].toInt
    val request: UserRequest = map("request").toString match {
      case "Create" => CreatePipeline
      case "Update" => UpdatePipeline
      case "Delete" => DeletePipeline
    }
    val preprocessors: Option[List[TransformerContainer]] = parse("preprocessors", map)
    val learner: Option[TransformerContainer] = {
      val l = parse("learner", map)
      if (l.isDefined) Some(l.get.head) else None
    }
    request match {
      case DeletePipeline =>
        collector.collect(ControlMessage(request, 0, pipelineId, None, Some(PipelineContainer(preprocessors, learner))))
      case _ =>
        if (preprocessors.isDefined || learner.isDefined)
          collector.collect(ControlMessage(request, 0, pipelineId, None, Some(PipelineContainer(preprocessors, learner))))
    }
  }

  private def parse(transformerName: String, map: Map[String, Any]): Option[List[TransformerContainer]] = {
    if (map.contains(transformerName)) {
      map(transformerName) match {

        case transformers: List[Map[String, Any]] =>
          if (transformers.nonEmpty) {
            val parsedList: List[TransformerContainer] =
              (for (transformer: Map[String, Any] <- transformers)
                yield {
                  PackagedTransformer(transformerName, transformer)
                }).flatten
            if (parsedList.nonEmpty) Some(parsedList) else None
          } else None

        case transformer: Map[String, Any] =>
          if (transformer.nonEmpty) {
            PackagedTransformer(transformerName, transformer) match {
              case Some(pkg: OML.message.packages.TransformerContainer) => Some(List(pkg))
              case None => None
            }
          } else None

        case null => None

        case _ => None
      }
    } else None
  }

  private def getTransformerName(transformerType: String, transformer: Map[String, Any]): Option[String] = {
    transformer("name").toString match {
      case nm: String => if (checkNameValidity(transformerType, nm)) Some(nm) else None
      case null => None
      case _ => None
    }
  }

  private def getTransformerHyperParameters(transformer: Map[String, Any]): Option[scala.collection.mutable.Map[String, Any]] = {
    if (transformer.contains("hyperparameters")) {
      transformer("hyperparameters") match {
        case params: Map[String, Any] =>
          if (params.isEmpty) None else Some(scala.collection.mutable.Map(params.toSeq: _*))
        case null => None
        case _ => None
      }
    } else None
  }

  private def getTransformerParameters(transformer: Map[String, Any]): Option[scala.collection.mutable.Map[String, Any]] = {
    if (transformer.contains("parameters")) {
      transformer("parameters") match {
        case w: Map[String, Any] => if (w.isEmpty) None else Some(scala.collection.mutable.Map(w.toSeq: _*))
        case null => None
        case _ => None
      }
    } else None
  }

  private def PackagedTransformer(transformerName: String, transformer: Map[String, Any]): Option[TransformerContainer] = {
    if (transformer.contains("name")) {
      val name: Option[String] = getTransformerName(transformerName, transformer)
      val hyperparameters = getTransformerHyperParameters(transformer)
      val parameters = getTransformerParameters(transformer)
      name match {
        case Some(nm: String) => Some(TransformerContainer(nm, hyperparameters, parameters))
        case None => None
      }
    } else None
  }

}

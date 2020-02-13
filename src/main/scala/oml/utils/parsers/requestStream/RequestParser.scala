package oml.utils.parsers.requestStream

import oml.message.ControlMessage
import oml.message.packages._
import org.apache.flink.api.common.functions.FlatMapFunction
import org.apache.flink.util.Collector

import scala.util.parsing.json.JSON

class RequestParser() extends FlatMapFunction[String, ControlMessage] {

  override def flatMap(in: String, collector: Collector[ControlMessage]): Unit = {
    try {
      val map = JSON.parseFull(in.stripMargin).get.asInstanceOf[Map[String, Any]]
      if (checkValidity(map)) parsePipeline(map, collector)
    } catch {
      case e: Exception =>
        println("Parsing failed")
        e.printStackTrace()
    }
  }

  private def checkValidity(map: Map[String, Any]): Boolean = {
    if (map.contains("id") && map.contains("request")) {
      checkRequestValidity(map)
    } else {
      ParsingErrorMessages.NoIdOrRequestKeys()
      false
    }
  }

  private def checkRequestValidity(map: Map[String, Any]): Boolean = {
    if (!ValidLists.requestNames.contains(map("request"))) {
      ParsingErrorMessages.RequestErrorMessage()
      false
    } else true
  }

  private def checkNameValidity(transformerType: String, name: String): Boolean = {
    transformerType match {
      case "preprocessors" => ValidLists.preprocessors.contains(name)
      case "learner" => ValidLists.learners.contains(name)
      case _ => false
    }
  }

  private def parsePipeline(map: Map[String, Any], collector: Collector[ControlMessage]): Unit = {
    val nodeId: Int = map("id").asInstanceOf[Double].toInt
    val request: Int = map("request").toString match {
      case "Create" => 0
      case "Inform" => -1
      case "Update" => -2
      case "Delete" => -3
    }
    request match {
      case 0 | -2 =>
        val learner: Option[TransformerContainer] = {
          val l = parse("learner", map)
          if (l.isDefined) Some(l.get.head) else None
        }
        if ((request == 0 && learner.isDefined) || request == 1) {
          val preprocessors: Option[List[TransformerContainer]] = parse("preprocessors", map)
          collector.collect(
            ControlMessage(request, 0, nodeId, None, Some(new MLWorkerConfig(preprocessors, learner)))
          )
        }
      case -1 | -3 =>
        collector.collect(ControlMessage(request, 0, nodeId, None, None))
    }
  }

  private def parse(transformerType: String, map: Map[String, Any]): Option[List[TransformerContainer]] = {
    if (map.contains(transformerType)) {
      map(transformerType) match {

        case transformers: List[Map[String, Any]] =>
          if (transformers.nonEmpty) {
            val parsedList: List[TransformerContainer] =
              (for (transformer: Map[String, Any] <- transformers)
                yield {
                  PackagedTransformer(transformerType, transformer)
                }).flatten
            if (parsedList.nonEmpty) Some(parsedList) else None
          } else None

        case transformer: Map[String, Any] =>
          if (transformer.nonEmpty) {
            PackagedTransformer(transformerType, transformer) match {
              case Some(pkg: oml.message.packages.TransformerContainer) => Some(List(pkg))
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
      case _ => None
    }
  }

  private def getParameters(parameterTypes: String, transformer: Map[String, Any])
  : Option[scala.collection.mutable.Map[String, Any]] = {
    def getP: Option[scala.collection.mutable.Map[String, Any]] = {
      transformer(parameterTypes) match {
        case params: Map[String, Any] =>
          if (params.isEmpty) None else Some(scala.collection.mutable.Map(params.toSeq: _*))
        case null => None
        case _ => None
      }
    }

    if (transformer.contains(parameterTypes) && ValidLists.parameterTypes.contains(parameterTypes)) getP else None
  }

  private def PackagedTransformer(transformerType: String, transformer: Map[String, Any])
  : Option[TransformerContainer] = {
    if (transformer.contains("name")) {
      getTransformerName(transformerType, transformer) match {
        case Some(nm: String) =>
          Some(TransformerContainer(nm,
            getParameters("hyperparameters", transformer),
            getParameters("parameters", transformer)))
        case None => None
      }
    } else None
  }

  object ParsingErrorMessages {
    def NoIdOrRequestKeys(): Unit = println("Parsing failed. A request must contain the keys \"id\" and \"request\".")

    def RequestErrorMessage(): Unit = println("Parsing failed. A request must contain one of the values " +
      "\"Create\", \"Update\", \"Delete\" for the key \"request\"")

    def preprocessorNameError(): Unit = println("Parsing failed. The provided preprocessor does not exists.")

    def learnerNameError(): Unit = println("Parsing failed. The provided preprocessor does not exist.")

    def transformerNameError(): Unit = println("Parsing failed. The provided transformers do not exist. " +
      "Acceptable transformer names are \"preprocessor\" and \"learner\".")
  }

  object ValidLists {
    val requestNames: List[String] = List("Create", "Inform", "Update", "Delete")
    val parameterTypes: List[String] = List("hyperparameters", "parameters")
    val preprocessors: List[String] = List("PolynomialFeatures", "StandardScaler")
    val learners: List[String] = List("PA", "regressorPA", "ORR")
  }

}

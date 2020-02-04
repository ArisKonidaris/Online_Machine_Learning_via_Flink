package OML.utils.parsers.requestStream

import OML.message.ControlMessage
import OML.message.packages.{TransformerContainer, _}

import org.apache.flink.api.common.functions.FlatMapFunction
import org.apache.flink.util.Collector

import scala.util.parsing.json.JSON
import scala.reflect.runtime.universe.{typeOf, TypeTag}

class RequestParser() extends FlatMapFunction[String, ControlMessage] {

  override def flatMap(in: String, collector: Collector[ControlMessage]): Unit = {
    val parse = JSON.parseFull(in.stripMargin)
    parse match {
      case Some(map: Map[String, Any]) =>
        try {
          if (checkValidity(map)) {
            if (checkRequestValidity(map)) parsePipeline(map, collector)
          } else ParsingErrorMessages.NoIdOrRequestKeys()
        } catch {
          case e: Exception =>
            println("Parsing failed")
            e.printStackTrace()
        }
      case _ => println("Unknown data structure")
    }
  }

  private def checkValidity(map: Map[String, Any]): Boolean = map.contains("id") && map.contains("request")

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
        collector.collect(
          ControlMessage(request, 0, pipelineId, None, Some(PipelineContainer(preprocessors, learner)))
        )
      case _ =>
        if (preprocessors.isDefined || learner.isDefined)
          collector.collect(
            ControlMessage(request, 0, pipelineId, None, Some(PipelineContainer(preprocessors, learner)))
          )
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
    val requestNames: List[String] = List("Create", "Update", "Delete")
    val parameterTypes: List[String] = List("hyperparameters", "parameters")
    val preprocessors: List[String] = List("PolynomialFeatures", "StandardScaler")
    val learners: List[String] = List("PA", "regressorPA", "ORR")
  }

  class CC[T] {
    def unapply(a: Any): Option[T] = Some(a.asInstanceOf[T])
  }

  object M extends CC[Map[String, Any]]

  object L extends CC[List[Any]]

  object S extends CC[String]

  object D extends CC[Double]

  object B extends CC[Boolean]

}

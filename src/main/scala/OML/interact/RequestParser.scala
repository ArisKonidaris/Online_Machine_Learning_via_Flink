package OML.interact

import org.apache.flink.api.common.functions.FlatMapFunction
import org.apache.flink.util.Collector

import scala.util.parsing.json.JSON

case class RequestParser() extends FlatMapFunction[String, String] {

  override def flatMap(in: String, collector: Collector[String]): Unit = {
    val parse = JSON.parseFull(in.stripMargin)
    println(parse)

    parse match {
      case Some(map: Map[String, Any]) =>
        try {
          if (checkRequestValidity(map)) {
            println(map)
            val id: Int = map("id").asInstanceOf[Double].toInt
            val request: String = map("request").toString
            request match {
              case "Create" => parsePreprocessors(map)
              case _ => println("Unknown request type")
            }
          } else println("Parsing failed. A request must contain the values \"id\" and \"request\".")
        } catch {
          case e: Exception =>
            println("Parsing failed")
            e.printStackTrace()
        }
      case None => println("Parsing failed")
      case other => println("Unknown data structure: " + other)
    }

  }

  private def checkRequestValidity(map: Map[String, Any]): Boolean = map.contains("id") && map.contains("request")

  private def parsePreprocessors(map: Map[String, Any]): Unit = {
    val preprocessors: Option[List[Map[String, Any]]] = {
      map("preprocessors") match {
        case p: List[Map[String, Any]] => Some(p)
        case null => None
        case _ => None
      }
    }
  }

  private def parseLearner(): Unit = ???

}

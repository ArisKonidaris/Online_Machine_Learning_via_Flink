package oml.utils.parsers.requestStream

sealed trait JSONValue

case class MapOfInts(value: Map[String, Int]) extends JSONValue

case class MapOfStrings(value: Map[String, String]) extends JSONValue

case class MapOfDoubles(value: Map[String, Double]) extends JSONValue

case class MapOfAny(value: Map[String, Any]) extends JSONValue

package oml.utils.parsers

case object StringToArrayDoublesParser extends GenericParser[String, Array[Double]] {
  override def parse(input: String): Array[Double] = input.split(",").map(_.toDouble)
}


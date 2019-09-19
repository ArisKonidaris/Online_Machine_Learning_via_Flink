package INFORE.utils.parsers

case object StringToArrayDoublesParser extends parser[String, Array[Double]] {
  override def parse(input: String): Array[Double] = input.split(",").map(_.toDouble)
}


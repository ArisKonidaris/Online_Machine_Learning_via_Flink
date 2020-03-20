package oml.mlAPI.parameters

/** Range for distributing the parameters to multiple parameter servers.
  *
  * @param start The first parameter.
  * @param end The last parameter.
  */
case class Range(protected var start: Int, protected var end: Int) {
  require(start >= 0 && start <= end)

  def this() = this(0, Int.MaxValue)

  def getStart: Int = start

  def getEnd: Int = end

  def setStart(start: Int): Unit = this.start = start

  def setEnd(end: Int): Unit = this.end = end

}

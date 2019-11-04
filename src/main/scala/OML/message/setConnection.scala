package OML.message


case class setConnection(override val partition: Int) extends ControlMessage {

  override def equals(obj: Any): Boolean = {
    obj match {
      case setConnection(part) => partition == part
      case _ => false
    }
  }

  override def toString: String = {
    s"setConnection($partition)"
  }

}
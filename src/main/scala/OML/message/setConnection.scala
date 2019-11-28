package OML.message


case class setConnection(part: Int) extends LearningMessage {

  var partition: Int = part

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
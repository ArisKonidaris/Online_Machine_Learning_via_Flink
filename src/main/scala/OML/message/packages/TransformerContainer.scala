package OML.message.packages

import scala.collection.mutable

/**
  * A case class for sending user request information to the nodes (workers/server) of the topology.
  *
  * @param name            The name of the method
  * @param hyperparameters The hyperparameters transmitted
  */
case class TransformerContainer(var name: String,
                                var hyperparameters: Option[mutable.Map[String, Any]],
                                var parameters: Option[mutable.Map[String, Any]])
  extends Serializable {

  // ====================================== Constructors ===========================================

  def this() = this("", None, None)

  // ==================================== Parameter methods ========================================

  /**
    * Retrieves a parameter value associated to a given key. The value is returned as an Option.
    *
    * @param parameter Key
    * @tparam T Type of the value to retrieve
    * @return Some(value) if an value is associated to the given key, otherwise the None value shall be returned
    */
  def get[T](parameter: String): Option[T] = {
    if (hyperparameters.get.isDefinedAt(parameter)) {
      hyperparameters.get(parameter).asInstanceOf[Option[T]]
    } else {
      None
    }
  }

  // ======================================== Getters ==============================================

  def getName: String = name

  def getHyperParameters: Option[mutable.Map[String, Any]] = hyperparameters

  def getParameters: Option[mutable.Map[String, Any]] = parameters

  // ======================================== Setters ==============================================

  def setName(name: String): Unit = this.name = name

  def setHyperParameters(parameters: Option[mutable.HashMap[String, Any]]): Unit = this.hyperparameters = parameters

  def setParameters(weights: Option[mutable.HashMap[String, Any]]): Unit = this.parameters = weights

  override def equals(obj: Any): Boolean = {
    obj match {
      case TransformerContainer(nm, hp, p) => name.equals(nm) && hyperparameters.equals(hp) && parameters.equals(p)
      case _ => false
    }
  }

  override def toString: String = {
    s"TransformerContainer($name, $hyperparameters, $parameters)"
  }

}

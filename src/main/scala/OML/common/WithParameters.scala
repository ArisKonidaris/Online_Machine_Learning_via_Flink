package OML.common

/**
  * Adds a [[ParameterMap]] which can be used to store configuration values
  */
trait WithParameters {
  val parameters = new ParameterMap
}


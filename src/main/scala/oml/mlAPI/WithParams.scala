package oml.mlAPI

import scala.collection.mutable

trait WithParams {
  def setHyperParameters(hyperParameterMap: mutable.Map[String, Any]): WithParams

  def addHyperParameter(key: String, value: Any): WithParams

  def removeHyperParameter(key: String, value: Any): WithParams

  def setParameters(parameterMap: mutable.Map[String, Any]): WithParams

  def addParameter(key: String, value: Any): WithParams

  def removeParameter(key: String, value: Any): WithParams
}

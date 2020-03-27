package oml.mlAPI

import scala.collection.mutable

trait WithParams {
  def setHyperParametersFromMap(hyperParameterMap: mutable.Map[String, AnyRef]): WithParams

  def addHyperParameter(key: String, value: AnyRef): WithParams

  def removeHyperParameter(key: String, value: AnyRef): WithParams

  def setParametersFromMap(parameterMap: mutable.Map[String, AnyRef]): WithParams

  def addParameter(key: String, value: AnyRef): WithParams

  def removeParameter(key: String, value: AnyRef): WithParams
}

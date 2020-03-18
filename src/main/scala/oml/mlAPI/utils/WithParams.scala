package oml.mlAPI.utils

import scala.collection.mutable

trait WithParams {
  def setHyperParameters(hyperParameterMap: mutable.Map[String, AnyRef]): WithParams

  def addHyperParameter(key: String, value: AnyRef): WithParams

  def removeHyperParameter(key: String, value: AnyRef): WithParams

  def setParameters(parameterMap: mutable.Map[String, AnyRef]): WithParams

  def addParameter(key: String, value: AnyRef): WithParams

  def removeParameter(key: String, value: AnyRef): WithParams
}

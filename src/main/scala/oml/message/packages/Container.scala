package oml.message.packages

import oml.mlAPI.WithParams

import scala.collection.mutable

trait Container extends Serializable with WithParams {
  override def setHyperParameters(hyperParameterMap: mutable.Map[String, Any]): Container = this

  override def addHyperParameter(key: String, value: Any): Container = this

  override def removeHyperParameter(key: String, value: Any): Container = this

  override def setParameters(parameterMap: mutable.Map[String, Any]): Container = this

  override def addParameter(key: String, value: Any): Container = this

  override def removeParameter(key: String, value: Any): Container = this
}
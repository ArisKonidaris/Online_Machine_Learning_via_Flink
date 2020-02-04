package OML.mlAPI

import scala.collection.mutable

trait WithParams {
  def setHyperParameters(hyperParameterMap: mutable.Map[String, Any]): WithParams

  def setParameters(parameterMap: mutable.Map[String, Any]): WithParams
}

package oml.mlAPI.utils

import oml.mlAPI.parameters.{LearningParameters, LinearModelParameters, MatrixModelParameters}

object LearningParameterUtils {

  def findLearningParameterClass(parameters: LearningParameters): Class[_] = {
    parameters match {
      case _: LinearModelParameters => LinearModelParameters.getClass
      case _: MatrixModelParameters => MatrixModelParameters.getClass
      case _ => LinearModelParameters.getClass
    }
  }

}

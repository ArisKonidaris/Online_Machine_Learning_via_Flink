package oml.mlAPI.parameters

import oml.math.{DenseVector, Vector}

import scala.collection.mutable.ListBuffer

/** A Serializable POJO case class for sending the parameters over the Network.
  * This class contains all the necessary information to reconstruct the parameters
  * on the receiver side.
  *
  * @param paramClass The class type of the parameters that implements the [[LearningParameters]] interface.
  * @param paramSizes The sizes of each sub parameter inside the LearningParameters instance.
  * @param params     The parameters in Serializable format.
  * @param fitted     The number of data used come up with these parameters.
  */
case class ParameterDescriptor(var paramClass: String,
                               var paramSizes: Array[Int],
                               var params: ListBuffer[Vector],
                               var ranges: ListBuffer[Range],
                               var fitted: Long)
  extends java.io.Serializable {

  def this() = this(LinearModelParameters.getClass.getName,
    Array(1, 1),
    ListBuffer(DenseVector(Array(0, 0))),
    ListBuffer(new Range()),
    0)

  def merge(pDesc: ParameterDescriptor): ParameterDescriptor = {
    this
  }

  def mergeRanges(newRanges: ListBuffer[Range]): Unit = {

  }

  def mergeParams(newParams: ListBuffer[Vector]): Unit = {

  }

  // =================================== Getters ===================================================

  def getParamClass: String = paramClass

  def getParamSizes: Array[Int] = paramSizes

  def getParams: ListBuffer[Vector] = params

  def getRanges: ListBuffer[Range] = ranges

  def getFitted: Long = fitted

  // =================================== Setters ===================================================

  def setParamClass(paramClass: String): Unit = this.paramClass = paramClass

  def setParamSizes(paramSizes: Array[Int]): Unit = this.paramSizes = paramSizes

  def setParams(params: ListBuffer[Vector]): Unit = this.params = params

  def setRanges(ranges: ListBuffer[Range]): Unit = this.ranges = ranges

  def setFitted(fitted: Long): Unit = this.fitted = fitted

}


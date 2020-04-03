package oml.mlAPI.mlParameterServers.parameterServers

import oml.FlinkBipartiteAPI.POJOs.Request
import oml.StarTopologyAPI.NodeInstance
import oml.mlAPI.parameters.ParameterDescriptor

/**
  * An abstract base class of a Machine Learning Parameter Server.
  *
  * @tparam WorkerIfc The remote interface of the Machine Learning worker.
  * @tparam QueryIfc The remote interface of the querier.
  */
abstract class MLParameterServer[WorkerIfc, QueryIfc] extends NodeInstance[WorkerIfc, QueryIfc] {

  /**
    * The cumulative loss of the distributed Machine Learning training.
    */
  protected var cumulativeLoss: Double = 0D

  /**
    * The number of data fitted to the distributed Machine Learning algorithm.
    */
  protected var fitted: Long = 0L

  /**
    * The range of parameters that the current parameter server is responsible for.
    */
  protected var parametersDescription: ParameterDescriptor = _

  // ================================================= Getters =========================================================

  def getCumulativeLoss: Double = cumulativeLoss

  def getNumberOfFittedData: Long = fitted

  def getParameterRange: ParameterDescriptor = parametersDescription

  // ================================================= Setters =========================================================

  def setCumulativeLoss(cumulativeLoss: Double): Unit = this.cumulativeLoss = cumulativeLoss

  def setNumberOfFittedData(fitted: Long): Unit = this.fitted = fitted

  def setParameterRange(parametersDescription: ParameterDescriptor): Unit =
    this.parametersDescription = parametersDescription

  // ============================== Machine Learning Parameter Server Basic Operations =================================

  /** This method configures the Parameter Server Node by using a creation Request.
    * Right now this method does not provide any functionality. It exists for configuring
    * more complex parameter server that may be developed later on. */
  def configureParameterServer(request: Request): MLParameterServer[WorkerIfc, QueryIfc] = {
    this
  }

  /** A method called when the Parameter Server needs to be cleared. */
  def clear(): MLParameterServer[WorkerIfc, QueryIfc] = {
    cumulativeLoss = 0D
    fitted = 0L
    parametersDescription = null
    this
  }

  def incrementNumberOfFittedData(size: Long): Unit = {
    if (fitted != Long.MaxValue) if (fitted < Long.MaxValue - size) fitted += size else fitted = Long.MaxValue
  }

}
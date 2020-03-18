package oml.mlAPI.mlParameterServers.parameterServers

import java.util

import oml.FlinkAPI.POJOs.Request
import oml.StarTopologyAPI.annotations.Inject
import oml.StarTopologyAPI.sites.NodeId
import oml.math.Point
import oml.mlAPI.mlpipeline.MLPipeline
import oml.mlAPI.parameters.LearningParameters

import scala.collection.mutable.ListBuffer

/**
  * An abstract base class of a Machine Learning Parameter Server.
  *
  * @tparam T The interface of the ml worker proxy.
  */
abstract class MLParameterServer[T] extends Serializable {

  /** A flag for determining if the parameter server is ready to serve. */
  protected var serving: Boolean = false

  /** The performance of the global machine learning model. */
  protected var performance: Double = 0D

  /** The cumulative loss of the distributed machine learning training. */
  protected var cumulativeLoss: Double = 0D

  /** The number of data fitted to the distributed machine learning algorithm. */
  protected var fitted: Long = 0L

  /** The local machine learning pipeline to train. */
  protected var global_ml_pipeline: MLPipeline = new MLPipeline()

  /** The range of parameters that the current parameter server is responsible for. */
  protected var parameterRange: (Int, Int) = (Int.MinValue, Int.MaxValue)

  /** The proxies for the remote workers. */
  @Inject
  protected var workerProxies: util.HashMap[NodeId, T] = _

  /** A broadcast proxy for the machine learning workers. */
  @Inject
  protected var workersBroadcastProxy: T = _

  // ====================================== Getters ================================================

  def getServing: Boolean = serving

  def getPerformance: Double = performance

  def getCumulativeLoss: Double = cumulativeLoss

  def getNumberOfFittedData: Long = fitted

  def getGlobalMLPipeline: MLPipeline = global_ml_pipeline

  def getParameterRange: (Int, Int) = parameterRange

  def getWorkerProxies: util.HashMap[NodeId, T] = workerProxies

  def getWorkersBroadcastProxy: T = workersBroadcastProxy

  def getGlobalLearnerParams: Option[LearningParameters] = global_ml_pipeline.getLearner.getParameters

  // ====================================== Setters ================================================

  def setServing(serving: Boolean): Unit = this.serving = serving

  def setPerformance(performance: Double): Unit = this.performance = performance

  def setCumulativeLoss(cumulativeLoss: Double): Unit = this.cumulativeLoss = cumulativeLoss

  def setNumberOfFittedData(fitted: Long): Unit = this.fitted = fitted

  def setGlobalMLPipeline(global_ml_pipeline: MLPipeline): Unit = this.global_ml_pipeline = global_ml_pipeline

  def setParameterRange(parameterRange: (Int, Int)): Unit = this.parameterRange = parameterRange

  def setWorkerProxies(workerProxies: util.HashMap[NodeId, T]): Unit = this.workerProxies = workerProxies

  def setGlobalLearnerParams(workersBroadcastProxy: T): Unit = this.workersBroadcastProxy  = workersBroadcastProxy

  def setGlobalLearnerParams(params: LearningParameters): Unit = global_ml_pipeline.getLearner.setParameters(params)

  // ========================= ML Parameter Server Basic Operations ================================

  /** This method configures a Online Machine Learning worker by using a creation Request */
  def configureParameterServer(request: Request): MLParameterServer[T] = {
    global_ml_pipeline.configureMLPipeline(request)
    this
  }

  /** A method called when the Parameter Server needs to be cleared. */
  def clear(): MLParameterServer[T] = {
    serving = false
    performance = 0D
    cumulativeLoss = 0D
    fitted = 0L
    global_ml_pipeline.clear()
    parameterRange = (Int.MinValue, Int.MaxValue)
    this
  }

  /**
    * A method for calculating the performance of the global ML pipeline.
    * @param test_set The test set to calculate the performance on.
    * @return
    */
  def getPerformance(test_set: ListBuffer[Point]): String = {
    global_ml_pipeline.score(test_set) match {
      case Some(score) => score + ""
      case None => "Can't calculate score"
    }
  }

  /** A method to check if the parameter server instance can serve. */
  def isServing: Boolean = getServing

}
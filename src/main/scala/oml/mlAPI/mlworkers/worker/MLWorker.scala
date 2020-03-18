package oml.mlAPI.mlworkers.worker

import java.util

import oml.FlinkAPI.POJOs.Request
import oml.StarTopologyAPI.annotations.Inject
import oml.StarTopologyAPI.sites.NodeId
import oml.math.Point
import oml.mlAPI.mlParameterServers.PullPush
import oml.mlAPI.mlpipeline.MLPipeline
import oml.mlAPI.parameters.LearningParameters

import scala.collection.mutable
import scala.collection.JavaConverters._
import scala.collection.mutable.ListBuffer

/**
  * An abstract base class of a Machine Learning worker
  *
  * @tparam T The interface of the parameter server proxy
  */
abstract class MLWorker[T <: PullPush]() extends Serializable {

  /** Total number of fitted data points to the local ML pipeline */
  protected var processed_data: Int = 0

  /** The size of the mini batch, or else, the number of distinct
    * data points that are fitted to the ML pipeline request a single fit operation
    */
  protected var mini_batch_size: Int = 64

  /** The number of mini-batches fitted by the worker checking
    * if the worker should send the parameters to the parameter server
    */
  protected var mini_batches: Int = 4

  /** The local machine learning pipeline to train */
  protected var ml_pipeline: MLPipeline = new MLPipeline()

  /** The global model */
  protected var global_model: LearningParameters = _

  /** The proxies to the parameter servers */
  @Inject
  protected var parameterServerProxies: util.HashMap[NodeId, T] = _

  /** A broadcast proxy for the parameter servers */
  @Inject
  protected var parameterServersBroadcastProxy: T = _

  // =================================== Getters ===================================================

  def getProcessedData: Int = processed_data

  def getMiniBatchSize: Int = mini_batch_size

  def getMiniBatches: Int = mini_batches

  def getMLPipeline: MLPipeline = ml_pipeline

  def getLearnerParams: Option[LearningParameters] = ml_pipeline.getLearner.getParameters

  def getGlobalModel: LearningParameters = global_model

  def getParameterServerProxies: util.HashMap[NodeId, T] = parameterServerProxies

  def getParameterServersBroadcastProxy: T = parameterServersBroadcastProxy

  // =================================== Setters ===================================================

  def setProcessedData(processed_data: Int): Unit = this.processed_data = processed_data

  def setMiniBatchSize(mini_batch_size: Int): Unit = this.mini_batch_size = mini_batch_size

  def setMiniBatches(mini_batches: Int): Unit = this.mini_batches = mini_batches

  def setMLPipeline(ml_pipeline: MLPipeline): Unit = this.ml_pipeline = ml_pipeline

  def setLearnerParams(params: LearningParameters): Unit = ml_pipeline.getLearner.setParameters(params)

  def setGlobalModel(global_model: LearningParameters): Unit = this.global_model = global_model

  def setDeepGlobalModel(global_model: LearningParameters): Unit = this.global_model = global_model.getCopy

  def setParameterServerProxies(psProxies: util.HashMap[NodeId, T]): Unit = this.parameterServerProxies = psProxies

  def setParameterServersBroadcastProxy(psbProxy: T): Unit = this.parameterServersBroadcastProxy = psbProxy

  // =================================== ML worker basic operations ================================

  /** This method configures a Online Machine Learning worker by using a creation Request */
  def configureWorker(request: Request): MLWorker[T] = {

    // Setting the ML node parameters
    val config: mutable.Map[String, AnyRef] = request.getTraining_configuration.asScala

    if (config == null) throw new RuntimeException("Empty training configuration map.")

    if (config.contains("mini_batch_size")) {
      try {
        setMiniBatchSize(config("mini_batch_size").asInstanceOf[Int])
      } catch {
        case e: Throwable => e.printStackTrace()
      }
    }

    if (config.contains("mini_batches")) {
      try {
        setMiniBatches(config("mini_batches").asInstanceOf[Double].toInt)
      } catch {
        case e: Throwable => e.printStackTrace()
      }
    }

    // Setting the ML pipeline
    ml_pipeline.configureMLPipeline(request)

    this
  }

  /** A method called when the ML worker needs to be cleared. */
  def clear(): MLWorker[T] = {
    processed_data = 0
    mini_batch_size = 64
    mini_batches = 4
    ml_pipeline.clear()
    global_model = null
    this
  }

  /** A method that returns the delta/shift of the
    * parameters since the last received global model.
    */
  def getDeltaVector: LearningParameters = {
    try {
      getLearnerParams.get - getGlobalModel
    } catch {
      case _: Throwable => getLearnerParams.get
    }
  }

  /** Initialization method of the ML worker
    *
    * @param data A data point for the initialization to be based on.
    * @return An [[MLWorker]] object
    */
  def init(data: Point): Unit = ml_pipeline.init(data)

  /**
    * A method to update the local models.
    */
  def updateModel(model: LearningParameters): Unit = {
    setGlobalModel(model)
    setLearnerParams(global_model.getCopy)
    setProcessedData(0)
  }

  /**
    * A method for calculating the performance of the local ML pipeline.
    * @param test_set The test set to calculate the performance on.
    * @return
    */
  def getPerformance(test_set: ListBuffer[Point]): String = {
    ml_pipeline.score(test_set) match {
      case Some(score) => score + ""
      case None => "Can't calculate score"
    }
  }

}

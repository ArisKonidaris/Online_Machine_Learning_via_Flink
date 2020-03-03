package oml.mlAPI.mlworkers.worker

import oml.POJOs.Request
import oml.StarProtocolAPI.Inject
import oml.logic.ParamServer
import oml.math.Point
import oml.mlAPI.dataBuffers.DataSet
import oml.mlAPI.mlpipeline.MLPipeline
import oml.parameters.LearningParameters

import scala.collection.mutable
import scala.collection.JavaConverters._

abstract class MLWorker() extends Serializable {

  // TODO: To be removed
  protected var nodeId: Int = -1

  // TODO: To be removed
  protected var flink_worker_id: Int = -1

  /** Total number of fitted data points to the local ML pipeline */
  protected var processed_data: Int = 0

  /** A flag determining if the local ML pipeline is allowed to fit new data.
    * When this is false, it means that the worker is waiting to
    * receive the new parameters from the parameter server
    */
  protected var process_data: Boolean = false

  /** The size of the mini batch, or else, the number of distinct
    * data points that are fitted to the ML pipeline request a single fit operation
    */
  protected var mini_batch_size: Int = 64

  /** The number of mini-batches fitted by the worker before
    * pushing the delta updates to the parameter server
    */
  protected var mini_batches: Int = 4

  /** The local machine learning pipeline to train */
  protected var ml_pipeline: MLPipeline = new MLPipeline()

  /** The global model */
  protected var global_model: LearningParameters = _

  /** The training data set buffer */
  protected var training_set: DataSet[Point] = new DataSet[Point]()

  /** A flag that determines whether the ML node has been merged with another */
  protected var merged: Boolean = false

  @Inject
  protected var ps: ParamServer = _

  // =================================== Getters ===================================================

  def getProcessedData: Int = processed_data

  def getProcessData: Boolean = process_data

  def getMiniBatchSize: Int = mini_batch_size

  def getMiniBatches: Int = mini_batches

  def getMLPipeline: MLPipeline = ml_pipeline

  def getLearnerParams: Option[LearningParameters] = ml_pipeline.getLearner.getParameters

  def getGlobalModel: LearningParameters = global_model

  def getTrainingSet: DataSet[Point] = training_set

  def getMerged: Boolean = merged

  // =================================== Setters ===================================================

  def setProcessedData(processed_data: Int): Unit = this.processed_data = processed_data

  def setProcessData(process_data: Boolean): Unit = this.process_data = process_data

  def setMiniBatchSize(mini_batch_size: Int): Unit = this.mini_batch_size = mini_batch_size

  def setMiniBatches(mini_batches: Int): Unit = this.mini_batches = mini_batches

  def setMLPipeline(ml_pipeline: MLPipeline): Unit = this.ml_pipeline = ml_pipeline

  def setLearnerParams(params: LearningParameters): Unit = ml_pipeline.getLearner.setParameters(params)

  def setGlobalModel(global_model: LearningParameters): Unit = this.global_model = global_model

  def setDeepGlobalModel(global_model: LearningParameters): Unit = this.global_model = global_model.getCopy

  def setTrainingSet(training_set: DataSet[Point]): Unit = this.training_set = training_set

  def setMerged(merged: Boolean): Unit = this.merged = merged

  // =================================== Periodic ML worker basic operations =======================

  def configureWorker(request: Request): MLWorker = {

    // TODO: Remove this fro here
    setNodeID(request.id)

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

    // Setting the ML worker flink_worker_id and acting accordingly
    if (config.contains("FlinkWorkerID")) {
      try {
        setFlinkWorkerID(config("FlinkWorkerID").asInstanceOf[Int])
        if (flink_worker_id == 0) setProcessData(true)
      } catch {
        case e: Throwable => e.printStackTrace()
      }
    } else throw new RuntimeException("No FlinkWorkerID given in training configuration map.")

    this
  }

  /** A method called when the ML worker needs to be cleared. */
  def clear(): MLWorker = {
    processed_data = 0
    process_data = false
    mini_batch_size = 64
    mini_batches = 4
    ml_pipeline.clear()
    global_model = null
    training_set.clear()
    this
  }

  /** A method called when merging two ML workers.
    *
    * @param worker The ML worker to merge this one with.
    * @return An [[MLWorker]] object
    */
  def merge(worker: MLWorker): MLWorker = {
    setMerged(true)
    setProcessedData(0)
    setProcessData(false)
    setMiniBatchSize(worker.getMiniBatchSize)
    setMiniBatches(worker.getMiniBatches)
    setMLPipeline(ml_pipeline.merge(worker.getMLPipeline))
    setGlobalModel(worker.getGlobalModel)
    setTrainingSet(training_set.merge(worker.getTrainingSet))
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
  def init(data: Point): Unit = {
    ml_pipeline.init(data)
  }

  // TODO: To be removed
  def setNodeID(nodeId: Int): Unit = this.nodeId = nodeId

  // TODO: To be removed
  def getNodeID: Int = nodeId

  // TODO: To be removed
  def setFlinkWorkerID(id: Int): Unit = this.flink_worker_id = id

  // TODO: To be removed
  def getFlinkWorkerID: Int = flink_worker_id

}

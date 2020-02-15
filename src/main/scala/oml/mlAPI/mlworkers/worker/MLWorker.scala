package oml.mlAPI.mlworkers.worker

import oml.StarProtocolAPI.Inject
import oml.logic.ParamServer
import oml.message.packages.MLWorkerConfig
import oml.message.workerMessage
import oml.mlAPI.dataBuffers.DataSet
import oml.mlAPI.mlpipeline.MLPipeline
import oml.parameters.LearningParameters
import org.apache.flink.util.Collector

import scala.collection.mutable

abstract class MLWorker() extends Serializable {

  /** An ID that uniquely defines a worker */
  protected var ID: String = "-1_-1"

  /** Total number of fitted data points to the local ML pipeline */
  protected var processed_data: Int = 0

  /** A flag determining if the local ML pipeline is allowed to fit new data.
    * When this is false, it means that the worker is waiting to
    * receive the new parameters from the parameter server
    */
  protected var process_data: Boolean = false

  /** The size of the mini batch, or else, the number of distinct
    * data points that are fitted to the ML pipeline in a single fit operation
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
  protected var training_set: DataSet = new DataSet()

  /** The message queue */
  protected var messageQueue: mutable.Queue[workerMessage] = new mutable.Queue[workerMessage]()

  /** A flag that determines whether the ML node has been merged with another */
  protected var merged: Boolean = false

  @Inject
  protected var ps: ParamServer = _

  // =================================== Getters ===================================================

  def getID: String = ID

  def getProcessedData: Int = processed_data

  def getProcessData: Boolean = process_data

  def getMiniBatchSize: Int = mini_batch_size

  def getMiniBatches: Int = mini_batches

  def getMLPipeline: MLPipeline = ml_pipeline

  def getLearnerParams: Option[LearningParameters] = ml_pipeline.getLearner.getParameters

  def getGlobalModel: LearningParameters = global_model

  def getTrainingSet: DataSet = training_set

  def getMessageQueue: mutable.Queue[workerMessage] = messageQueue

  def getMerged: Boolean = merged


  // =================================== Setters ===================================================

  def setID(id: String): Unit = ID = id

  def setProcessedData(processed_data: Int): Unit = this.processed_data = processed_data

  def setProcessData(process_data: Boolean): Unit = this.process_data = process_data

  def setMiniBatchSize(mini_batch_size: Int): Unit = this.mini_batch_size = mini_batch_size

  def setMiniBatches(mini_batches: Int): Unit = this.mini_batches = mini_batches

  def setMLPipeline(ml_pipeline: MLPipeline): Unit = this.ml_pipeline = ml_pipeline

  def setLearnerParams(params: LearningParameters): Unit = ml_pipeline.getLearner.setParameters(params)

  def setGlobalModel(global_model: LearningParameters): Unit = this.global_model = global_model

  def setDeepGlobalModel(global_model: LearningParameters): Unit = this.global_model = global_model.getCopy

  def setTrainingSet(training_set: DataSet): Unit = this.training_set = training_set

  def setMessageQueue(messageQueue: mutable.Queue[workerMessage]): Unit = this.messageQueue = messageQueue

  def setMerged(merged: Boolean): Unit = this.merged = merged

  // =================================== Periodic ML worker basic operations =======================

  def configureWorker(container: MLWorkerConfig): MLWorker = {

    // Setting the ML node parameters
    val config: mutable.Map[String, Any] = container.getParameters
    if (config.contains("id")) {
      try {
        setID(config("id").toString)
      } catch {
        case e: Throwable => e.printStackTrace()
      }
    }
    if (config.contains("mini_batch_size")) {
      try {
        setMiniBatchSize(config("mini_batch_size").asInstanceOf[Double].toInt)
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
    ml_pipeline.configureMLPipeline(container)

    // Setting the ML worker id and acting accordingly
    if (ID.split("_")(0).toInt == 0) setProcessData(true) else pull()

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
    messageQueue.clear()
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

  /** Request the global model from the parameter server
    */
  def pull(): Unit = messageQueue.enqueue(new workerMessage(ID.split("_")(1).toInt, ID.split("_")(0).toInt))

  def send(out: Collector[workerMessage]): Unit = {
    while (messageQueue.nonEmpty) out.collect(messageQueue.dequeue())
  }

}

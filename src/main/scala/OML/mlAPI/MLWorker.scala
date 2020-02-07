package OML.mlAPI

import java.io

import OML.StarProtocolAPI.Node
import OML.math.Point
import OML.message.packages.MLPipelineContainer
import OML.message.workerMessage
import OML.mlAPI.dataBuffers.TrainingSet
import OML.mlAPI.dprotocol.{DistributedProtocol, PeriodicMLWorker}
import OML.mlAPI.mlpipeline.MLPipeline
import OML.parameters.LearningParameters

import scala.collection.mutable
import scala.collection.mutable.ListBuffer

case class MLWorker() extends Node with Serializable {

  /** An ID that uniquely defines a worker */
  protected var ID: String = _

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

  /** The communication strategy of the ML worker */
  protected var protocol: DistributedProtocol = _

  /** The local machine learning pipeline to train */
  protected var ml_pipeline: MLPipeline = new MLPipeline()

  /** The training data set buffer */
  protected var global_model: LearningParameters = _

  protected var training_set: TrainingSet = new TrainingSet()

  /** The message queue */
  protected var messageQueue: mutable.Queue[workerMessage] = new mutable.Queue[workerMessage]()

  // =================================== Getters ===================================================

  def getID: String = ID

  def getProcessedData: Int = processed_data

  def getProcessData: Boolean = process_data

  def getMiniBatchSize: Int = mini_batch_size

  def getMiniBatches: Int = mini_batches

  def getMLPipeline: MLPipeline = ml_pipeline

  def getLearnerParams: Option[LearningParameters] = ml_pipeline.getLearner.getParameters

  def getGlobalModel: LearningParameters = global_model

  def getTrainingSet: TrainingSet = training_set

  def getMessageQueue: mutable.Queue[workerMessage] = messageQueue


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

  def setTrainingSet(training_set: TrainingSet): Unit = this.training_set = training_set

  def setMessageQueue(messageQueue: mutable.Queue[workerMessage]): Unit = this.messageQueue = messageQueue

  // =================================== Periodic ML worker basic operations =======================

  def configureWorker(com_proto: String, container: MLPipelineContainer): MLWorker = {
    // TODO: Remember to configure the rest of the periodic ML worker variables like mini batch size etc.
    if (DistributedProtocol.protocols.contains(com_proto)) {
      protocol = {
        com_proto match {
          case "Asynchronous" => PeriodicMLWorker()
          case "Synchronous" => PeriodicMLWorker()
          case "DynamicAveraging" => PeriodicMLWorker()
          case "FGM" => PeriodicMLWorker()
          case _ => PeriodicMLWorker()
        }
      }
    } else protocol = PeriodicMLWorker()
    ml_pipeline.configureMLPipeline(container)
    this
  }

  def configureWorker(container: MLPipelineContainer): MLWorker = configureWorker("Asynchronous", container)

  /** Initialization method of the ML worker
    *
    * @param data A data point for the initialization to be based on.
    * @return An [[MLWorker]] object
    */
  def init(data: Point): MLWorker = {
    ml_pipeline.init(data)
    setProcessData(true)
    this
  }

  /** A method called when the ML worker needs to be cleared. */
  def clear(): Unit = {
    processed_data = 0
    process_data = false
    mini_batch_size = 64
    mini_batches = 4
    ml_pipeline.clear()
    global_model = null
    training_set.clear()
    messageQueue.clear()
  }

  /** A method called when merging two ML workers.
    *
    * @param worker The ML worker to merge this one with.
    * @return An [[MLWorker]] object
    */
  def merge(worker: MLWorker): MLWorker = {
    processed_data = 0
    process_data = false
    mini_batch_size = worker.getMiniBatchSize
    mini_batches = worker.getMiniBatches
    ml_pipeline.merge(worker.getMLPipeline)
    global_model = worker.getGlobalModel
    training_set.merge(worker.getTrainingSet)
    this
  }

  /** A verbose calculation of the score of the ML pipeline.
    *
    * @param test_set The test set that the score should be calculated on.
    * @return A human readable text for observing the training of the ML method.
    */
  def scoreVerbose(test_set: ListBuffer[Point]): String = {
    s"$ID, ${
      ml_pipeline.score(test_set) match {
        case Some(score) => score
        case None => "Can't calculate score"
      }
    }, ${training_set.length}, ${test_set.length}"
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

  /** A method called each type the new global
    * model arrives from the parameter server.
    */
  def updateModel(model: LearningParameters): Unit = {
    setGlobalModel(model)
    setLearnerParams(global_model.getCopy)
    setProcessData(true)
    fitFromBuffer()
  }

  /** The consumption of a data point by the ML worker.
    *
    * @param data A data point to be fitted to the ML pipeline
    */
  def processPoint(data: Point): Unit = {
    if (process_data && training_set.isEmpty) {
      ml_pipeline.fit(data)
      processed_data += 1
    } else {
      training_set.append(data)
    }
    fitFromBuffer()
  }

  def fitFromBuffer(): Unit = {
    if (process_data) {

      val batch_size: Int = mini_batch_size * mini_batches
      while (processed_data < batch_size && training_set.nonEmpty) {
        val batch_len: Int = Math.min(batch_size - processed_data, training_set.length)
        ml_pipeline.fit(training_set.getDataSet.slice(0, batch_len))
        training_set.getDataSet.remove(0, batch_len)
        processed_data += batch_len
      }

      if (processed_data >= mini_batch_size * mini_batches) {
        if (protocol.sendToPS()) {
          setProcessData(false)
          messageQueue.enqueue(workerMessage(pipelineID = ID.split("_")(1).toInt,
            workerId = ID.split("_")(0).toInt,
            parameters = getDeltaVector,
            request = 1))
        }
        setProcessedData(0)
      }
    }
  }

  override def receiveMsg(operation: Integer, message: io.Serializable): Unit = ???

  override def receiveTuple(tuple: io.Serializable): Unit = {
    try {
      val data: Point = tuple.asInstanceOf[Point]
      processPoint(data)
    } catch {
      case _: Throwable => println("Wrong tuple type. Should be a OML.math.Point.")
    }
  }

  override def receiveControlMessage(request: io.Serializable): Unit = ???


}

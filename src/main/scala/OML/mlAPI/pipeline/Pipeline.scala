package OML.mlAPI.pipeline

import OML.common.OMLTools.mergeBufferedPoints
import OML.math.Point
import OML.message.packages.{PipelineContainer, TransformerContainer}
import OML.message.workerMessage
import OML.mlAPI.WithParams
import OML.mlAPI.learners.Learner
import OML.mlAPI.learners.classification.PA
import OML.mlAPI.learners.regression.{ORR, regressorPA}
import OML.mlAPI.preprocessing.{PolynomialFeatures, StandardScaler, preProcessing}
import OML.parameters.LearningParameters

import scala.collection.mutable
import scala.collection.mutable.ListBuffer
import scala.util.Random

case class Pipeline(private var preprocess: ListBuffer[preProcessing],
                    private var learner: Learner)
  extends Serializable {

  import Pipeline._

  // =================================== Private variables =========================================

  /** An ID that uniquely defines a pipeline */
  private var ID: String = _

  /** The number of data points fitted to this ML pipeline */
  private var fitted_data: Int = 0

  /** Total number of fitted data points at the current worker */
  private var processed_data: Int = 0

  /** A flag determining if the learner is allowed to fit new data.
    * When this is false, it means that the learner is waiting to
    * receive the new hyperparameters from the coordinator
    */
  private var process_data: Boolean = false

  /** The size of the mini batch, or else, the number of distinct
    * data points that are fitted to the learner in a single fit operation
    */
  private var mini_batch_size: Int = 64

  /** The number of mini-batches fitted by the worker before
    * pushing the delta updates to the coordinator
    */
  private var mini_batches: Int = 4

  /** The training data set buffer */
  private var training_set: ListBuffer[Point] = ListBuffer[Point]()

  /** The capacity of the data point buffer used for training
    * the local model. This is done to prevent overflow */
  private var train_set_max_size: Int = 500000

  /** The training data set buffer */
  private var global_model: LearningParameters = _

  /** The number of times this pipeline has been merged with other ones */
  private var merges: Int = 0

  /** The message queue */
  private var messageQueue: mutable.Queue[Serializable] = new mutable.Queue[Serializable]()

  def this() = this(ListBuffer[preProcessing](), null)

  // =========================== Pipeline creation/interaction methods =============================

  def addPreprocessor(preprocessor: preProcessing): Pipeline = {
    preprocess = preprocess :+ preprocessor
    this
  }

  def addPreprocessor(preprocessor: preProcessing, index: Int): Pipeline = {
    preprocess = (preprocess.slice(0, index) :+ preprocessor) ++ preprocess.slice(index, preprocess.length)
    this
  }

  def removePreprocessor(index: Int): Pipeline = {
    preprocess = preprocess.slice(0, index) ++ preprocess.slice(index + 1, preprocess.length)
    this
  }

  def addLearner(learner: Learner): Pipeline = {
    this.learner = learner
    this
  }

  def removeLearner(): Pipeline = {
    this.learner = null
    this
  }

  private def matchPreprocessor(container: TransformerContainer): preProcessing = {
    var preProcessor: preProcessing = null
    container.getName match {
      case "PolynomialFeatures" => preProcessor = new PolynomialFeatures
      case "StandardScaler" => preProcessor = new StandardScaler
      case _ => None
    }
    preProcessor
  }

  private def matchLearner(container: TransformerContainer): Learner = {
    var learner: Learner = null
    container.getName match {
      case "PA" => learner = new PA
      case "regressorPA" => learner = new regressorPA
      case "ORR" => learner = new ORR
      case _ => None
    }
    learner
  }

  private def configTransformer(transformer: WithParams, container: TransformerContainer): Unit = {
    container.getHyperParameters match {
      case Some(hparams: mutable.Map[String, Any]) => transformer.setHyperParameters(hparams)
      case None =>
    }
    container.getParameters match {
      case Some(params: mutable.Map[String, Any]) => transformer.setParameters(params)
      case None =>
    }
  }

  private def createPreProcessor(container: TransformerContainer): preProcessing = {
    val transformer: preProcessing = matchPreprocessor(container)
    configTransformer(transformer, container)
    transformer
  }

  private def createLearner(container: TransformerContainer): Learner = {
    val transformer: Learner = matchLearner(container)
    configTransformer(transformer, container)
    transformer
  }

  def configurePipeline(container: PipelineContainer): Pipeline = {
    val ppContainer: Option[List[TransformerContainer]] = container.getPreprocessors
    ppContainer match {
      case Some(lppContainer: List[TransformerContainer]) =>
        for (pp: TransformerContainer <- lppContainer)
          addPreprocessor(createPreProcessor(pp))
      case None =>
    }

    val lContainer: Option[TransformerContainer] = container.getLearner
    lContainer match {
      case Some(lContainer: TransformerContainer) => addLearner(createLearner(lContainer))
      case None =>
    }
    this
  }

  // =================================== ML pipeline basic operations ==============================

  def init(data: Point): Pipeline = {
    require(learner != null, "The pipeline must have a learner to fit")
    pipePoint(data, preprocess, learner.initialize_model)
    setProcessData(true)
    this
  }

  def clear(): Unit = {
    fitted_data = 0
    processed_data = 0
    process_data = false
    mini_batch_size = 64
    mini_batches = 4
    train_set_max_size = 500000
    training_set.clear()
    global_model = null
    merges = 0
    preprocess.clear()
    learner = null
  }

  private def fit(data: Point): Unit = {
    require(learner != null, "The pipeline must have a learner to fit data.")
    pipePoint(data, preprocess, learner.fit)
    incrementFitCount()
  }

  private def fit(mini_batch: ListBuffer[Point]): Unit = {
    require(learner != null, "The pipeline must have a learner to fit data.")
    pipePoints(mini_batch, preprocess, learner.fit)
    incrementFitCount(mini_batch.length)
  }

  def predict(data: Point): Option[Double] = {
    require(learner != null, "The pipeline must have a learner make a prediction.")
    pipePoint(data, preprocess, learner.predict)
  }

  def score(testSet: ListBuffer[Point]): Option[Double] = {
    require(learner != null, "Cannot calculate performance. The pipeline doesn't contain a learner.")
    pipePoints(testSet, preprocess, learner.score)
  }

  def scoreVerbose(test_set: ListBuffer[Point]): String = {
    s"$ID, ${
      score(test_set) match {
        case Some(score) => score
        case None => "Can't calculate score"
      }
    }, ${training_set.length}, ${test_set.length}"
  }

  /** A method that returns the delta/shift of the parameters since the last received global model. */
  private def getDeltaVector: LearningParameters = {
    try {
      getLearnerParams.get - getGlobalModel
    } catch {
      case _: Throwable => getLearnerParams.get
    }
  }

  private def incrementFitCount(): Unit = if (fitted_data < Int.MaxValue) fitted_data += 1

  private def incrementFitCount(mini_batch: Int): Unit = {
    if (fitted_data < Int.MaxValue - mini_batch) fitted_data += mini_batch else fitted_data = Int.MaxValue
  }

  // =================================== ML pipeline merging operations ============================

  def merge(pipeline: Pipeline): Pipeline = {
    fitted_data = if (fitted_data + pipeline.getFittedData < Int.MaxValue)
      fitted_data + pipeline.getFittedData
    else
      Int.MaxValue
    merges += 1
    processed_data = 0
    process_data = false
    mini_batch_size = pipeline.getMiniBatchSize
    mini_batches = pipeline.getMiniBatches
    train_set_max_size = pipeline.getTrainingSetMaxSize
    global_model = pipeline.getGlobalModel
    preprocess = pipeline.getPreprocessors
    learner = pipeline.getLearner

    if (pipeline.getTrainingSet.nonEmpty) {
      if (training_set.isEmpty) {
        training_set = pipeline.getTrainingSet
      } else {
        training_set = mergeBufferedPoints(1, training_set.length,
          0, pipeline.getTrainingSet.length,
          training_set, pipeline.getTrainingSet,
          merges)
        while (training_set.length > train_set_max_size)
          training_set.remove(Random.nextInt(training_set.length))
      }
    }

    this
  }

  def completeMerge(): Unit = merges = 0

  // ============================= Data point buffer management methods ============================

  /** Method that prevents memory overhead due to the data point buffer.
    *
    * The worker cannot train on any data while waiting for the response of the parameter
    * server with the new global model, so it cashes any new data point in that time. This
    * method monitors the size of that buffer. If the buffer becomes too large, the oldest
    * data point is discarded to prevent memory overhead.
    *
    */
  private def overflowCheck(): Unit = {
    if (training_set.length > train_set_max_size)
      training_set.remove(Random.nextInt(train_set_max_size + 1))
  }

  def appendToTrainSet(data: Point): Unit = {
    training_set += data
    overflowCheck()
  }

  private def insertToTrainSet(index: Int, data: Point): Unit = {
    training_set.insert(index, data)
    overflowCheck()
  }

  // =================================== ML pipeline API methods =============================

  def updateModel(model: LearningParameters): Unit = {
    global_model = model
    learner.setParameters(global_model.getCopy)
    setProcessedData(0)
    setProcessData(true)
    bulkFit()
  }

  def processPoint(data: Point): Unit = {
    if (process_data && training_set.isEmpty) {
      fit(data)
      processed_data += 1
    } else {
      appendToTrainSet(data)
    }
    bulkFit()
  }

  def bulkFit(): Unit = {
    if (process_data) {
      val batch_size: Int = mini_batch_size * mini_batches
      while (processed_data < batch_size && training_set.nonEmpty) {
        val batch_len: Int = Math.min(batch_size - processed_data, training_set.length)
        fit(training_set.slice(0, batch_len))
        training_set.remove(0, batch_len)
        processed_data += batch_len
      }
      if (checkIfMessageToServerIsNeeded()) {
        setProcessData(false)
        messageQueue.enqueue(workerMessage(pipelineID = ID.split("_")(1).toInt,
          workerId = ID.split("_")(0).toInt,
          parameters = getDeltaVector,
          request = 1))
      }
    }
  }

  /** Method determining if the worker needs to pull the global
    * hyperparameters from the parameter server for this ML pipeline.
    *
    * For the default asynchronous distributed ML, the worker pulls the
    * hyperparameters periodically, after the fitting of a constant number of data points.
    *
    * @return Whether to request the global hyperparameters from the parameter server
    */
  def checkIfMessageToServerIsNeeded(): Boolean = processed_data >= mini_batch_size * mini_batches

  // =================================== Getters ===================================================

  def getID: String = ID

  def getFittedData: Int = fitted_data

  def getMiniBatchSize: Int = mini_batch_size

  def getMiniBatches: Int = mini_batches

  def getTrainingSetMaxSize: Int = train_set_max_size

  def getTrainingSet: ListBuffer[Point] = training_set

  def getGlobalModel: LearningParameters = global_model

  def getPreprocessors: ListBuffer[preProcessing] = preprocess

  def getPreprocessor(index: Int): preProcessing = preprocess(index)

  def getLearner: Learner = learner

  def getLearnerParams: Option[LearningParameters] = learner.getParameters

  def getMessageQueue: mutable.Queue[Serializable] = messageQueue

  // =================================== Setters ===================================================

  def setID(id: String): Unit = ID = id

  def fittedData(count: Int): Unit = fitted_data = count

  def setProcessData(process_data: Boolean): Unit = this.process_data = process_data

  def setProcessedData(processed_data: Int): Unit = this.processed_data = processed_data

  def setMiniBatchSize(mini_batch_size: Int): Unit = this.mini_batch_size = mini_batch_size

  def setMiniBatches(mini_batches: Int): Unit = this.mini_batches = mini_batches

  def setTrainingSetMaxSize(size: Int): Unit = this.train_set_max_size = size

  def setTrainingSet(training_set: ListBuffer[Point]): Unit = this.training_set = training_set

  def setDeepTrainingSet(training_set: ListBuffer[Point]): Unit = this.training_set = training_set.clone()

  def setTrainSetMaxSize(train_set_max_size: Int): Unit = this.train_set_max_size = train_set_max_size

  def setGlobalModel(global_model: LearningParameters): Unit = this.global_model = global_model

  def setDeepGlobalModel(global_model: LearningParameters): Unit = this.global_model = global_model.getCopy

  def setMessageQueue(messageQueue: mutable.Queue[Serializable]): Unit = this.messageQueue = messageQueue

}

object Pipeline {

  // =================================== Factory methods ===========================================

  def apply(): Pipeline = new Pipeline()

  // ====================================== Operations =============================================

  @scala.annotation.tailrec
  final def pipePoint[T](data: Point, list: ListBuffer[preProcessing], f: Point => T): T = {
    if (list.isEmpty) f(data) else pipePoint(list.head.transform(data), list.tail, f)
  }

  @scala.annotation.tailrec
  final def pipePoints[T](data: ListBuffer[Point], list: ListBuffer[preProcessing], f: ListBuffer[Point] => T): T = {
    if (list.isEmpty) f(data) else pipePoints(list.head.transform(data), list.tail, f)
  }

}

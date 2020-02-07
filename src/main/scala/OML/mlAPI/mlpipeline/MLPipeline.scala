package OML.mlAPI.mlpipeline

import OML.math.Point
import OML.message.packages.{MLPipelineContainer, TransformerContainer}
import OML.mlAPI.WithParams
import OML.mlAPI.learners.Learner
import OML.mlAPI.learners.classification.PA
import OML.mlAPI.learners.regression.{ORR, regressorPA}
import OML.mlAPI.preprocessing.{PolynomialFeatures, StandardScaler, preProcessing}

import scala.collection.mutable
import scala.collection.mutable.ListBuffer

case class MLPipeline(private var preprocess: ListBuffer[preProcessing], private var learner: Learner)
  extends Serializable {

  import MLPipeline._

  def this() = this(ListBuffer[preProcessing](), null)

  /** The number of data points fitted to the ML pipeline */
  private var fitted_data: Int = 0

  // =================================== Getters ===================================================

  def getPreprocessors: ListBuffer[preProcessing] = preprocess

  def getLearner: Learner = learner

  def getFittedData: Int = fitted_data

  // =================================== Setters ===================================================

  def setPreprocessors(preprocess: ListBuffer[preProcessing]): Unit = this.preprocess = preprocess

  def setLearner(learner: Learner): Unit = this.learner = learner

  def setFittedData(fitted_data: Int): Unit = this.fitted_data = fitted_data

  // =========================== ML Pipeline creation/interaction methods =============================

  def addPreprocessor(preprocessor: preProcessing): MLPipeline = {
    preprocess = preprocess :+ preprocessor
    this
  }

  def addPreprocessor(preprocessor: preProcessing, index: Int): MLPipeline = {
    preprocess = (preprocess.slice(0, index) :+ preprocessor) ++ preprocess.slice(index, preprocess.length)
    this
  }

  def removePreprocessor(index: Int): MLPipeline = {
    preprocess = preprocess.slice(0, index) ++ preprocess.slice(index + 1, preprocess.length)
    this
  }

  def addLearner(learner: Learner): MLPipeline = {
    this.learner = learner
    this
  }

  def removeLearner(): MLPipeline = {
    this.learner = null
    this
  }

  def matchPreprocessor(container: TransformerContainer): preProcessing = {
    var preProcessor: preProcessing = null
    container.getName match {
      case "PolynomialFeatures" => preProcessor = new PolynomialFeatures
      case "StandardScaler" => preProcessor = new StandardScaler
      case _ => None
    }
    preProcessor
  }

  def matchLearner(container: TransformerContainer): Learner = {
    var learner: Learner = null
    container.getName match {
      case "PA" => learner = new PA
      case "regressorPA" => learner = new regressorPA
      case "ORR" => learner = new ORR
      case _ => None
    }
    learner
  }

  def configTransformer(transformer: WithParams, container: TransformerContainer): Unit = {
    container.getHyperParameters match {
      case Some(hparams: mutable.Map[String, Any]) => transformer.setHyperParameters(hparams)
      case None =>
    }
    container.getParameters match {
      case Some(params: mutable.Map[String, Any]) => transformer.setParameters(params)
      case None =>
    }
  }

  def createPreProcessor(container: TransformerContainer): preProcessing = {
    val transformer: preProcessing = matchPreprocessor(container)
    configTransformer(transformer, container)
    transformer
  }

  def createLearner(container: TransformerContainer): Learner = {
    val transformer: Learner = matchLearner(container)
    configTransformer(transformer, container)
    transformer
  }

  def configureMLPipeline(container: MLPipelineContainer): MLPipeline = {
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

  def init(data: Point): MLPipeline = {
    require(learner != null, "The ML pipeline must have a learner to fit")
    pipePoint(data, preprocess, learner.initialize_model)
    this
  }

  def clear(): Unit = {
    fitted_data = 0
    preprocess.clear()
    learner = null
  }

  def fit(data: Point): Unit = {
    require(learner != null, "The mlpipeline must have a learner to fit data.")
    pipePoint(data, preprocess, learner.fit)
    incrementFitCount()
  }

  def fit(mini_batch: ListBuffer[Point]): Unit = {
    require(learner != null, "The mlpipeline must have a learner to fit data.")
    pipePoints(mini_batch, preprocess, learner.fit)
    incrementFitCount(mini_batch.length)
  }

  def predict(data: Point): Option[Double] = {
    require(learner != null, "The mlpipeline must have a learner make a prediction.")
    pipePoint(data, preprocess, learner.predict)
  }

  def score(testSet: ListBuffer[Point]): Option[Double] = {
    require(learner != null, "Cannot calculate performance. The mlpipeline doesn't contain a learner.")
    pipePoints(testSet, preprocess, learner.score)
  }

  private def incrementFitCount(mini_batch: Int): Unit = {
    if (fitted_data < Int.MaxValue - mini_batch) fitted_data += mini_batch else fitted_data = Int.MaxValue
  }

  private def incrementFitCount(): Unit = incrementFitCount(1)

  def merge(mlPipeline: MLPipeline): MLPipeline = {
    fitted_data = {
      if (fitted_data + mlPipeline.getFittedData < Int.MaxValue)
        fitted_data + mlPipeline.getFittedData
      else
        Int.MaxValue
    }
    preprocess = mlPipeline.getPreprocessors
    learner = mlPipeline.getLearner
    this
  }

}

object MLPipeline {

  // =================================== Factory methods ===========================================

  def apply(): MLPipeline = new MLPipeline()

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

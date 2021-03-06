package oml.mlAPI.mlpipeline

import oml.POJOs.{Preprocessor, Request}
import oml.math.Point
import oml.mlAPI.WithParams
import oml.mlAPI.learners.Learner
import oml.mlAPI.learners.classification.PA
import oml.mlAPI.learners.regression.{ORR, regressorPA}
import oml.mlAPI.preprocessing.{PolynomialFeatures, StandardScaler, preProcessing}
import oml.parameters.{Bucket, ParameterDescriptor}

import scala.collection.mutable
import scala.collection.mutable.ListBuffer
import scala.collection.JavaConverters._

case class MLPipeline(private var preprocess: ListBuffer[preProcessing], private var learner: Learner)
  extends Serializable {

  import MLPipeline._

  def this() = this(ListBuffer[preProcessing](), null)

  /** The number of data points fitted to the ML pipeline. */
  private var fitted_data: Long = 0

  // =================================== Getters ===================================================

  def getPreprocessors: ListBuffer[preProcessing] = preprocess

  def getLearner: Learner = learner

  def getFittedData: Long = fitted_data

  // =================================== Setters ===================================================

  def setPreprocessors(preprocess: ListBuffer[preProcessing]): Unit = this.preprocess = preprocess

  def setLearner(learner: Learner): Unit = this.learner = learner

  def setFittedData(fitted_data: Long): Unit = this.fitted_data = fitted_data

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

  def matchPreprocessor(preprocessor: Preprocessor): Option[preProcessing] = {
    var preProcessor: Option[preProcessing] = null
    preprocessor.getName match {
      case "PolynomialFeatures" => preProcessor = Some(PolynomialFeatures())
      case "StandardScaler" => preProcessor = Some(StandardScaler())
      case _ => None
    }
    preProcessor
  }

  def matchLearner(estimator: oml.POJOs.Learner): Learner = {
    estimator.getName match {
      case "PA" => new PA
      case "regressorPA" => new regressorPA
      case "ORR" => new ORR
      case _ => null
    }
  }

  def configTransformer(transformer: WithParams, preprocessor: oml.POJOs.Transformer): Unit = {
    val hparams: mutable.Map[String, AnyRef] = preprocessor.getHyperparameters.asScala
    if (hparams != null) transformer.setHyperParametersFromMap(hparams)

    val params: mutable.Map[String, AnyRef] = preprocessor.getParameters.asScala
    if (params != null) transformer.setParametersFromMap(params)
  }

  def createPreProcessor(preprocessor: Preprocessor): Option[preProcessing] = {
    matchPreprocessor(preprocessor) match {
      case Some(transformer: preProcessing) =>
        configTransformer(transformer, preprocessor)
        Some(transformer)
      case None => None
    }
  }

  def createLearner(learner: oml.POJOs.Learner): Learner = {
    val transformer: Learner = matchLearner(learner)
    configTransformer(transformer, learner)
    transformer
  }

  def configureMLPipeline(request: Request): MLPipeline = {
    try {
      val ppContainer: List[Preprocessor] = request.getPreprocessors.asScala.toList
      for (pp: Preprocessor <- ppContainer)
        createPreProcessor(pp) match {
          case Some(preprocessor: preProcessing) => addPreprocessor(preprocessor)
          case None =>
        }
    } catch {
      case _: java.lang.NullPointerException =>
      case other: Throwable => other.printStackTrace()
    }

    try {
      val lContainer: oml.POJOs.Learner = request.getLearner
      if (lContainer != null) addLearner(createLearner(lContainer))
    } catch {
      case _: java.lang.NullPointerException =>
      case other: Throwable => other.printStackTrace()
    }

    this
  }

  // =================================== ML pipeline operations ==============================

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
    require(learner != null, "The ML pipeline must have a learner to fit data.")
    pipePoint(data, preprocess, learner.fit)
    incrementFitCount()
  }

  def fit(mini_batch: ListBuffer[Point]): Unit = {
    require(learner != null, "The mlpipeline must have a learner to fit data.")
    pipePoints(mini_batch, preprocess, learner.fit)
    incrementFitCount(mini_batch.length.asInstanceOf[Long])
  }

  def predict(data: Point): Option[Double] = {
    require(learner != null, "The mlpipeline must have a learner make a prediction.")
    pipePoint(data, preprocess, learner.predict)
  }

  def score(testSet: ListBuffer[Point]): Option[Double] = {
    require(learner != null, "Cannot calculate performance. The mlpipeline doesn't contain a learner.")
    pipePoints(testSet, preprocess, learner.score)
  }

  private def incrementFitCount(mini_batch: Long): Unit = {
    if (fitted_data < Long.MaxValue - mini_batch) fitted_data += mini_batch else fitted_data = Long.MaxValue
  }

  private def incrementFitCount(): Unit = incrementFitCount(1)

  def merge(mlPipeline: MLPipeline): MLPipeline = {
    incrementFitCount(mlPipeline.getFittedData)
    preprocess = mlPipeline.getPreprocessors
    learner = mlPipeline.getLearner
    this
  }

  def generateDescriptor(): ParameterDescriptor = {
    if (learner != null && learner.getParameters.isDefined) {
      val (sizes, parameters, bucket) = getLearner
        .getSerializedParams(getLearner.getParameters.get, false, Bucket(0,getLearner.getParameters.get.getSize - 1))
      new ParameterDescriptor(sizes, parameters, bucket, fitted_data)
    } else new ParameterDescriptor()
  }

  def generatePOJO: (List[Preprocessor], oml.POJOs.Learner, Long) = {
    val prPJ = (for (preprocessor <- getPreprocessors) yield preprocessor.generatePOJOPreprocessor).toList
    val lrPJ = getLearner.generatePOJOLearner
    (prPJ, lrPJ, fitted_data)
  }

  def generatePOJO(testSet: ListBuffer[Point]): (List[Preprocessor], oml.POJOs.Learner, Long, Double) = {
    val genPJ = generatePOJO
    (genPJ._1,
      genPJ._2,
      genPJ._3,
      score(testSet) match {
        case Some(value: Double) => value
        case None => Long.MinValue
      }
    )
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

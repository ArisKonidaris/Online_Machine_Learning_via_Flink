package oml.mlAPI.mlworkers.worker

import oml.POJOs.{DataInstance, Prediction, Request}
import oml.StarTopologyAPI.{Inject, MergeOp, QueryOp, ReceiveTuple}
import oml.mlAPI.Investigator
import oml.math.{DenseVector, Point, UnlabeledPoint}
import oml.mlAPI.mlpipeline.MLPipeline
import oml.mlAPI.mlworkers.MLWorkerRemote
import oml.parameters.{LearningParameters, ParameterDescriptor}

import scala.collection.mutable
import scala.collection.JavaConverters._

class MLPredictor() extends Serializable with MLWorkerRemote {

  // TODO: To be removed
  protected var nodeId: Int = -1

  /** The local machine learning pipeline predictor */
  protected var ml_pipeline: MLPipeline = new MLPipeline()

  @Inject
  protected var querier: Investigator = _

  // =================================== Getters ===================================================

  def getMLPipeline: MLPipeline = ml_pipeline

  def getLearnerParams: Option[LearningParameters] = ml_pipeline.getLearner.getParameters

  // =================================== Setters ===================================================

  def setMLPipeline(ml_pipeline: MLPipeline): Unit = this.ml_pipeline = ml_pipeline

  def setLearnerParams(params: LearningParameters): Unit = ml_pipeline.getLearner.setParameters(params)

  // =================================== Periodic ML workers basic operations =======================

  def configureWorker(request: Request): MLPredictor = {

    // TODO: Remove this from here
    setNodeID(request.id)

    // Setting the ML node parameters
    val config: mutable.Map[String, AnyRef] = request.getTraining_configuration.asScala
    if (config == null) throw new RuntimeException("Empty training configuration map.")

    // Setting the ML pipeline
    ml_pipeline.configureMLPipeline(request)

    this
  }

  /** A method called when the ML predictors need to be cleared. */
  def clear(): MLPredictor = {
    ml_pipeline.clear()
    this
  }

  /** A method called when merging two ML predictors.
    *
    * @param worker The ML workers to merge this one with.
    * @return An [[MLWorker]] object
    */
  @MergeOp
  def merge(worker: MLWorker): MLPredictor = {
    setMLPipeline(ml_pipeline.merge(worker.getMLPipeline))
    this
  }

  /** Initialization method of the ML predictor
    *
    * @param data A data point for the initialization to be based on.
    * @return An [[MLWorker]] object
    */
  def init(data: Point): Unit = ml_pipeline.init(data)


  // TODO: To be removed
  def setNodeID(nodeId: Int): Unit = this.nodeId = nodeId

  // TODO: To be removed
  def getNodeID: Int = nodeId

  /** The data point to be predicted.
    *
    * @param data A data point to be fitted to the ML pipeline
    */
  @ReceiveTuple
  def receiveTuple(data: DataInstance): Unit = {

    val unlabeledPoint = UnlabeledPoint(
      DenseVector(data.getNumericFeatures.asInstanceOf[java.util.List[Double]].asScala.toArray)
    )

    val prediction = {
      ml_pipeline.predict(unlabeledPoint) match {
        case Some(prediction: Double) => prediction
        case None => Double.MaxValue
      }
    }

    querier.sendPrediction(new Prediction(nodeId, data, prediction))

  }

  /** A method called each type the new global
    * model arrives from the parameter server.
    */
  override def updateModel(modelDescriptor: ParameterDescriptor): Unit = {
    setLearnerParams(ml_pipeline.getLearner.generateParameters(modelDescriptor))
  }

  /** This method responds to a query for the ML pipeline.
    *
    * @param test_set The test set that the predictive performance of the model should be calculated on.
    * @return A human readable text for observing the training of the ML method.
    */
  @QueryOp
  def query(queryId: Long, test_set: Array[java.io.Serializable]): Unit = {
  }

}

package OML.common

import OML.learners.Learner
import OML.math.Point
import OML.parameters.LearningParameters
import OML.preprocessing.preProcessing

import scala.collection.mutable.ListBuffer

case class PipelineState() extends Serializable {

  // =================================== Private variables =========================================

  /** An ID that uniquely defines a pipeline */
  var ID: String = _

  /** Total number of fitted data points at the current worker */
  var processed_data: Int = _

  /** A flag determining if the learner is allowed to fit new data. */
  var process_data: Boolean = _

  /** The size of the mini batch, or else, the number of distinct
    * data points that are fitted to the learner in a single fit operation
    */
  var mini_batch_size: Int = _

  /** The number of mini-batches fitted by the worker before
    * pushing the delta updates to the coordinator
    */
  var mini_batches: Int = _

  /** The training data set buffer */
  var training_set: ListBuffer[Point] = _

  /** The capacity of the data point buffer used for training
    * the local model. This is done to prevent overflow */
  var train_set_max_size: Int = _

  /** The training data set buffer */
  var global_model: LearningParameters = _

  /** A list of concatenated preprocessors */
  var preprocess: ListBuffer[preProcessing] = _

  /** An ML algorithm */
  var learner: Learner = _

  // =================================== Setters ===================================================

  def setID(id: String): Unit = ID = id

  def setProcessData(process_data: Boolean): Unit = this.process_data = process_data

  def setProcessedData(processed_data: Int): Unit = this.processed_data = processed_data

  def setMiniBatchSize(mini_batch_size: Int): Unit = this.mini_batch_size = mini_batch_size

  def setMiniBatches(mini_batches: Int): Unit = this.mini_batches = mini_batches

  def setTrainingSet(training_set: ListBuffer[Point]): Unit = this.training_set = training_set.clone()

  def setTrainSetMaxSize(train_set_max_size: Int): Unit = this.train_set_max_size = train_set_max_size

  def setGlobalModel(global_model: LearningParameters): Unit = this.global_model = global_model.getCopy

  // =================================== Getters ===================================================

  def getMiniBatchSize: Int = mini_batch_size

  def getMiniBatches: Int = mini_batches

  def getLearner: Learner = learner

  def getPreprocessors: ListBuffer[preProcessing] = preprocess

  def getPreprocessor(index: Int): preProcessing = preprocess(index)

  def getTrainingSet: ListBuffer[Point] = training_set

  def getGlobalModel: LearningParameters = global_model

}

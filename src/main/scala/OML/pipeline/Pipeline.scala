package OML.pipeline

import OML.learners.Learner
import OML.math.Point
import OML.preprocessing.preProcessing

import scala.collection.mutable.ListBuffer

case class Pipeline(private var preprocess: ListBuffer[preProcessing], private var learner: Learner)
  extends Serializable {

  import Pipeline._

  def this() = this(ListBuffer[preProcessing](), null)

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

  def init(data: Point): Pipeline = {
    require(learner != null, "The pipeline must have a learner to fit")
    pipePoint(data, preprocess, learner.initialize_model)
    this
  }

  def fit(data: Point): Unit = {
    require(learner != null, "The pipeline must have a learner to fit")
    pipePoint(data, preprocess, learner.fit)
  }

  def fit(batch: ListBuffer[Point]): Unit = {
    require(learner != null, "The pipeline must have a learner to fit")
    pipePoints(batch, preprocess, learner.fit)
  }

  def predict(data: Point): Option[Double] = {
    require(learner != null, "The pipeline must have a learner to fit")
    pipePoint(data, preprocess, learner.predict)
  }

  def score(testSet: ListBuffer[Point]): Option[Double] = {
    require(learner != null, "The pipeline must have a learner to fit")
    pipePoints(testSet, preprocess, learner.score)
  }

  def getLearner: Learner = learner

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

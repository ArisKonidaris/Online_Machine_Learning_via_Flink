package OML.pipeline

import OML.learners.Learner
import OML.math.Point
import OML.preprocessing.{learningPreprocessor, preprocessing}

case class Pipeline(var preprocess: List[preprocessing], var learner: Learner) extends Serializable {

  import Pipeline._

  def this() = this(List[preprocessing](), null)

  def addPreprocessor(preprocessor: preprocessing): Pipeline = {
    preprocess = preprocess :+ preprocessor
    this
  }

  def addPreprocessor(preprocessor: preprocessing, index: Int): Pipeline = {
    preprocess = preprocess.slice(0, index) :: preprocessor :: preprocess.slice(index, preprocess.length)
    this
  }

  def removePreprocessor(index: Int): Pipeline = {
    preprocess = preprocess.slice(0, index) :: preprocess.slice(index + 1, preprocess.length)
    this
  }

  def addLearner(learner: Learner): Pipeline = {
    this.learner = learner
    this
  }

  def fit(data: Point): Unit = {
    require(learner != null, "The pipeline must have a learner to fit")
    pipeFit(data, preprocess, learner.fit)
  }

  def predict(data: Point): Option[Double] = {
    require(learner != null, "The pipeline must have a learner to fit")
    pipeFit(data, preprocess, learner.predict)
  }

}

object Pipeline {

  // =================================== Factory methods ===========================================

  def apply(): Pipeline = new Pipeline()

  // ====================================== Operations =============================================

  @scala.annotation.tailrec
  final def pipeFit[T](data: Point, list: List[preprocessing], f: Point => T): T = {
    if (list.isEmpty) f(data) else pipeFit(list.head.transform(data), list.tail, f)
  }

}

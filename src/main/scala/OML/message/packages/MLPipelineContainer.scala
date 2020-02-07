package OML.message.packages

case class MLPipelineContainer(var preprocessors: Option[List[TransformerContainer]],
                               var learner: Option[TransformerContainer])
  extends Container {
  def this() = this(None, None)

  def setPreprocessors(preprocessors: Option[List[TransformerContainer]]): Unit = this.preprocessors = preprocessors

  def setLearner(learner: Option[TransformerContainer]): Unit = this.learner = learner

  def getPreprocessors: Option[List[TransformerContainer]] = preprocessors

  def getLearner: Option[TransformerContainer] = learner

  override def equals(obj: Any): Boolean = {
    obj match {
      case MLPipelineContainer(pp, l) =>
        preprocessors.equals(pp) && learner.equals(l)
      case _ => false
    }
  }

  override def toString: String = {
    s"MLPipelineContainer($preprocessors, $learner)"
  }

}

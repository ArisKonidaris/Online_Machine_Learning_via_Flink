package OML.protocol

import OML.logic.{Worker, PS}
import OML.mlAPI.learners.Learner

case class AsynchronousCoProto()
  extends LearningProto {
  override def workerLogic: Worker = new Worker

  override def psLogic: PS = new PS
}

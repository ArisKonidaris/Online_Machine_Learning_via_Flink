package INFORE.message

import org.apache.flink.api.scala.typeutils.TraversableSerializer.Key

/** Base trait for messages received by the workers.
  *
  */
trait LearningMessage extends Serializable {
  val partition: Int = 0
}

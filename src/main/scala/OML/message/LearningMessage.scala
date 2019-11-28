package OML.message

import org.apache.flink.api.scala.typeutils.TraversableSerializer.Key

/** Base trait for messages received by the workers.
  *
  */
trait LearningMessage extends Serializable {
  var partition: Int

  def getPartition: Int = partition

  def setPartition(partition: Int): Unit = this.partition = partition

}

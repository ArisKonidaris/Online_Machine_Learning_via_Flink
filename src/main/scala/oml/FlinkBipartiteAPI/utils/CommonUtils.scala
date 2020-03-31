package oml.FlinkBipartiteAPI.utils

import oml.FlinkBipartiteAPI.messages.{ControlMessage, SpokeMessage}
import org.apache.flink.streaming.api.scala.StreamExecutionEnvironment

import scala.collection.mutable.ListBuffer

object CommonUtils {

  /** Registers the different FlinkML related protocols for Kryo serialization
    *
    * @param env The Flink execution environment where the protocols need to be registered
    */
  def registerFlinkMLTypes(env: StreamExecutionEnvironment): Unit = {

    // oml Point protocols
    env.registerType(classOf[oml.mlAPI.math.Point])
    env.registerType(classOf[oml.mlAPI.math.LabeledPoint])
    env.registerType(classOf[oml.mlAPI.math.UnlabeledPoint])

    // Vector protocols
    env.registerType(classOf[oml.mlAPI.math.DenseVector])
    env.registerType(classOf[oml.mlAPI.math.SparseVector])

    // oml message protocols
    env.registerType(classOf[SpokeMessage])
    env.registerType(classOf[ControlMessage])

  }

  /**
    * Tail recursive method for merging two data buffers.
    */
  @scala.annotation.tailrec
  def mergeBufferedPoints[T <: java.io.Serializable](count1: Int, size1: Int,
                                                     count2: Int, size2: Int,
                                                     set1: ListBuffer[T], set2: ListBuffer[T],
                                                     offset: Int): ListBuffer[T] = {
    if (count2 == size2) {
      set1
    } else if (count1 == size1) {
      set1 ++ set2
    } else {
      set1.insert(count1, set2(count2))
      mergeBufferedPoints(count1 + 1 + offset, size1, count2 + 1, size2, set1, set2, offset)
    }
  }

}

package oml.common

import oml.message.mtypes.{ControlMessage, DataPoint, workerMessage}
import org.apache.flink.streaming.api.scala.StreamExecutionEnvironment

import scala.collection.mutable.ListBuffer

object OMLTools {

  /** Registers the different FlinkML related types for Kryo serialization
    *
    * @param env The Flink execution environment where the types need to be registered
    */
  def registerFlinkMLTypes(env: StreamExecutionEnvironment): Unit = {

    // oml Point types
    env.registerType(classOf[oml.math.Point])
    env.registerType(classOf[oml.math.LabeledPoint])
    env.registerType(classOf[oml.math.UnlabeledPoint])

    // Vector types
    env.registerType(classOf[oml.math.DenseVector])
    env.registerType(classOf[oml.math.SparseVector])

    // oml message types
    env.registerType(classOf[DataPoint])
    env.registerType(classOf[workerMessage])
    env.registerType(classOf[ControlMessage])

    // oml learning parameter types
    env.registerType(classOf[oml.mlAPI.parameters.LearningParameters])
    env.registerType(classOf[oml.mlAPI.parameters.LinearModelParameters])
    env.registerType(classOf[oml.mlAPI.parameters.MatrixModelParameters])

    // Matrix types
    env.registerType(classOf[oml.math.DenseMatrix])
    env.registerType(classOf[oml.math.SparseMatrix])

    // Breeze Vector types
    env.registerType(classOf[breeze.linalg.DenseVector[_]])
    env.registerType(classOf[breeze.linalg.SparseVector[_]])

    // Breeze specialized types
    env.registerType(breeze.linalg.DenseVector.zeros[Double](0).getClass)
    env.registerType(breeze.linalg.SparseVector.zeros[Double](0).getClass)

    // Breeze Matrix types
    env.registerType(classOf[breeze.linalg.DenseMatrix[Double]])
    env.registerType(classOf[breeze.linalg.CSCMatrix[Double]])

    // Breeze specialized types
    env.registerType(breeze.linalg.DenseMatrix.zeros[Double](0, 0).getClass)
    env.registerType(breeze.linalg.CSCMatrix.zeros[Double](0, 0).getClass)
  }

  /**
    * Tail recursive method for merging two data point buffers (Either training or testing ones).
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

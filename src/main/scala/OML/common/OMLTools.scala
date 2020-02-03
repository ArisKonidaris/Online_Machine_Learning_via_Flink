package OML.common

import org.apache.flink.streaming.api.scala.StreamExecutionEnvironment

import scala.collection.mutable.ListBuffer

object OMLTools {

  /** Registers the different FlinkML related types for Kryo serialization
    *
    * @param env The Flink execution environment where the types need to be registered
    */
  def registerFlinkMLTypes(env: StreamExecutionEnvironment): Unit = {

    // OML Point types
    env.registerType(classOf[OML.math.Point])
    env.registerType(classOf[OML.math.LabeledPoint])
    env.registerType(classOf[OML.math.UnlabeledPoint])

    // Vector types
    env.registerType(classOf[OML.math.DenseVector])
    env.registerType(classOf[OML.math.SparseVector])

    // OML message types
    env.registerType(classOf[OML.message.DataPoint])
    env.registerType(classOf[OML.message.workerMessage])
    env.registerType(classOf[OML.message.ControlMessage])

    // OML learning parameter types
    env.registerType(classOf[OML.parameters.LearningParameters])
    env.registerType(classOf[OML.parameters.LinearModelParameters])
    env.registerType(classOf[OML.parameters.MatrixLinearModelParameters])

    // Matrix types
    env.registerType(classOf[OML.math.DenseMatrix])
    env.registerType(classOf[OML.math.SparseMatrix])

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
  def mergeBufferedPoints(count1: Int, size1: Int,
                          count2: Int, size2: Int,
                          set1: ListBuffer[OML.math.Point], set2: ListBuffer[OML.math.Point],
                          offset: Int): ListBuffer[OML.math.Point] = {
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

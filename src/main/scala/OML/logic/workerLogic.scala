package OML.logic

import OML.message.{DataPoint, LearningMessage, psMessage}
import OML.parameters.{LearningParameters, LinearModelParameters}
import breeze.linalg.{DenseVector => BreezeDenseVector}
import org.apache.flink.api.common.functions.FlatMapFunction
import org.apache.flink.ml.common.LabeledVector
import org.apache.flink.ml.math.Breeze._
import org.apache.flink.util.Collector

import scala.collection.mutable.ListBuffer
import scala.util.Random

class workerLogic extends FlatMapFunction[LearningMessage, (Int, Int, LearningParameters)] {

  /** The id of the current worker/slave */
  private var worker_id: Int = -1

  /** Used to sample data points for testing the accuracy of the model */
  private var count: Int = 0

  /** Total number of fitted data points at the current worker */
  private var processed_data: Int = 0

  /** A flag determining if the learner is allowed to fit new data.
    * When this is false, it means that the learner is waiting to
    * receive the new parameters from the coordinator
    */
  private var process_data: Boolean = false

  /** The number of data points fitted by the worker before
    * pushing the delta updates to the coordinator
    */
  private val batch_size: Int = 256

  /** The capacity of the data point buffer used for testing the performance
    * of the local model. This is done to prevent overflow */
  private val test_set_size: Int = 1000

  /** The capacity of the data point buffer used for training
    * the local model. This is done to prevent overflow */
  private val train_set_size: Int = 500000

  /** The training data set buffer */
  private val training_set: ListBuffer[LabeledVector] = ListBuffer[LabeledVector]()

  /** The test set buffer */
  private var test_set: ListBuffer[LabeledVector] = ListBuffer[LabeledVector]()

  /** The local and last global learning parameters */
  private var model: LearningParameters = _
  private var global_model: LearningParameters = _
  Random.setSeed(25)

  val c: Double = 0.01

  override def flatMap(input: LearningMessage, out: Collector[(Int, Int, LearningParameters)]): Unit = {
    input match {
      case DataPoint(partition, data) =>

        // Initializations
        try {
          require(partition == worker_id, s"message partition $partition integer does not equal worker ID $worker_id")
        } catch {
          case e: Exception =>
            if (worker_id < 0) {
              setWorkerId(partition)
              if (partition == 0) {
                model = init_model(data)
                process_data = true
              }
            } else {
              throw new IllegalArgumentException(e.getMessage)
            }
        }

        if (count >= 8) {

          test_set += data
          if (test_set.length > test_set_size) {
            training_set += test_set.remove(0)
            overflowCheck()
          }

        } else {

          // Data point trigger functionality
          if (training_set.isEmpty && process_data) {
            fit(data)
            processed_data += 1
          } else {
            training_set += data
            overflowCheck()
          }

        }

      case psMessage(partition, data) =>
        try {
          require(partition == worker_id, s"message partition integer $partition does not equal worker ID $worker_id")
        } catch {
          case e: Exception =>
            if (worker_id < 0) {
              setWorkerId(partition)
            } else {
              throw new IllegalArgumentException(e.getMessage)
            }
        }

        updateLocalModel(data)
        process_data = true

    }
    count += 1
    if (count == 10) count = 0

    if (process_data) {
      if (processed_data < batch_size && training_set.nonEmpty) {
        val batch_len: Int = Math.min(batch_size - processed_data, training_set.length)
        fit(training_set.slice(0, batch_len))
        training_set.remove(0, batch_len)
        processed_data += batch_len
      }

      if (checkIfMessageToServerIsNeeded()) sendModelToServer(out)
      //      if (training_set.isEmpty) println(worker_id)
    }

    //    accuracy(worker_id)
  }

  private def predict(data: LabeledVector): Option[Double] = {
    try {
      Some(
        (data.vector.asBreeze dot model.asInstanceOf[LinearModelParameters].weights)
          + model.asInstanceOf[LinearModelParameters].intercept
      )
    } catch {
      case _: Throwable => None
    }
  }

  private def fit(data: LabeledVector): Unit = {
    val label = if (data.label == 0.0) -1.0 else data.label
    val loss: Double = 1.0 - label * predict(data).get

    if (loss > 0.0) {
      val Lagrange_Multiplier: Double = loss / (((data.vector dot data.vector) + 1.0) + 1 / (2 * c))
      model += LinearModelParameters(
        (Lagrange_Multiplier * label * data.vector.asBreeze).asInstanceOf[BreezeDenseVector[Double]],
        Lagrange_Multiplier * label
      )
    }
  }

  private def fit(batch: ListBuffer[LabeledVector]): Unit = for (point <- batch) fit(point)

  private def accuracy(partition: Int): Unit = {
    try {
      if (Random.nextFloat() >= 0.95 && model != null) {
        val accuracy: Double = (for (test <- test_set)
          yield {
            val prediction = if (predict(test).get >= 0.0) 1.0 else 0.0
            if (test.label == prediction) 1 else 0
          }).sum / (1.0 * test_set.length)

        println(partition, accuracy, training_set.length)
      }
    } catch {
      case _: Throwable => println(s"$partition can't produce score")
    }
  }

  private def updateLocalModel(data: LearningParameters): Unit = {
    global_model = data
    model = global_model.getCopy()
  }

  private def init_model(data: LabeledVector): LearningParameters = {
    LinearModelParameters(BreezeDenseVector.zeros[Double](data.vector.size), 0.0)
  }

  private def sendModelToServer(out: Collector[(Int, Int, LearningParameters)]): Unit = {
    processed_data = 0
    process_data = false

    val mdl: LearningParameters = {
      try {
        model - global_model
      } catch {
        case _: Throwable => model
      }
    }

    out.collect((0, worker_id, mdl))
  }

  private def setWorkerId(id: Int): Unit = worker_id = id

  private def checkIfMessageToServerIsNeeded(): Boolean = processed_data == batch_size

  private def overflowCheck(): Unit = {
    if (training_set.length > train_set_size) training_set.remove(Random.nextInt(train_set_size + 1))
  }

}
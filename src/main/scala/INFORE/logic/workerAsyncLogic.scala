package INFORE.logic

import INFORE.common.{LabeledPoint, Point}
import INFORE.learners.Learner
import INFORE.message.{DataPoint, LearningMessage, psMessage}
import INFORE.nodes.WorkerNode.WorkerLogic
import INFORE.parameters.{LearningParameters, LinearModelParameters}
import breeze.linalg.{DenseVector => BreezeDenseVector}
import org.apache.flink.ml.math.Breeze._
import org.apache.flink.util.Collector

import scala.collection.mutable
import scala.util.Random

class workerAsyncLogic[L <: Learner : Manifest]
  extends WorkerLogic[LearningMessage, (Int, Int, LearningParameters), L] {

  private var worker_id: Int = -1

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

  /** The training data set buffer */
  private val training_set: mutable.Queue[Point] = mutable.Queue[Point]()

  /** The test set buffer */
  private var test_set: Array[Point] = Array[Point]()

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

        if (Random.nextFloat() > 0.8) {

          test_set = test_set :+ data
          if (test_set.length > 10000)
            test_set = test_set.slice(1, test_set.length)

        } else {

          // Data point trigger functionality
          if (training_set.isEmpty && process_data) {
            fit(data)
            processed_data += 1
          } else {
            training_set.enqueue(data)
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

    if(process_data) {
      while (processed_data < batch_size && training_set.nonEmpty) {
        fit(training_set.dequeue())
        processed_data += 1
      }

      if (checkIfMessageToServerIsNeeded()) sendModelToServer(out)
      if(training_set.isEmpty) println(worker_id)
    }

    score(worker_id)
  }

  def fit(data: Point): Unit = {
    val parameters: LinearModelParameters = model.asInstanceOf[LinearModelParameters]
    val label = if (data.asInstanceOf[LabeledPoint].label == 0.0) -1.0 else data.asInstanceOf[LabeledPoint].label
    val loss: Double = 1.0 - label * ((data.vector.asBreeze dot parameters.weights) + parameters.intercept)

    if (loss > 0.0) {
      val Lagrange_Multiplier: Double = loss / (((data.vector dot data.vector) + 1.0) + 1 / (2 * c))
      model = LinearModelParameters(parameters.weights + Lagrange_Multiplier * label * data.vector.asBreeze,
        parameters.intercept + Lagrange_Multiplier * label)
    }
  }

  private def score(partition: Int): Unit = {
    try{
      if (Random.nextFloat() >= 0.99) {
        val parameters: LinearModelParameters = model.asInstanceOf[LinearModelParameters]
        val accuracy: Double = (for (test <- test_set)
          yield {
            val prediction = if ((test.vector.asBreeze dot parameters.weights) + parameters.intercept >= 0.0) 1.0 else 0.0
            if (test.asInstanceOf[LabeledPoint].label == prediction) 1 else 0
          }).sum / (1.0 * test_set.length)

        println(partition, accuracy, training_set.length)
      }
    } catch {
      case _: Throwable => println(s"$partition can't produce score")
    }
  }

  override def updateLocalModel(data: LearningParameters): Unit = {
    global_model = data
    model = data
  }

  private def init_model(data: Point): LearningParameters = {
    LinearModelParameters(BreezeDenseVector.zeros[Double](data.vector.size), 0.0)
  }

  override def sendModelToServer(out: Collector[(Int, Int, LearningParameters)]): Unit = {
    processed_data = 0
    process_data = false

    val mdl: LearningParameters = {
      try {
        model - global_model
      } catch {
        case e: Throwable => model
      }
    }

    out.collect((0, worker_id, mdl))
  }

  override def setWorkerId(id: Int): Unit = worker_id = id

  override def checkIfMessageToServerIsNeeded(): Boolean = processed_data == batch_size

}
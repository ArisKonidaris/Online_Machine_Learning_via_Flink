package INFORE.logic

import INFORE.message.{DataPoint, LearningMessage, psMessage}
import INFORE.parameters.{LearningParameters => l_params, LinearModelParameters => lin_params}
import breeze.linalg.{DenseVector => BreezeDenseVector}
import org.apache.flink.api.common.functions.FlatMapFunction
import org.apache.flink.api.common.state.{ListState, ListStateDescriptor}
import org.apache.flink.api.common.typeinfo.{TypeHint, TypeInformation}
import org.apache.flink.ml.common.LabeledVector
import org.apache.flink.ml.math.Breeze._
import org.apache.flink.runtime.state.{FunctionInitializationContext, FunctionSnapshotContext}
import org.apache.flink.streaming.api.checkpoint.CheckpointedFunction
import org.apache.flink.streaming.api.scala.createTypeInformation
import org.apache.flink.util.Collector

import scala.collection.mutable
import scala.collection.mutable.ListBuffer
import scala.util.Random

class CheckWorker
  extends FlatMapFunction[LearningMessage, (Int, Int, l_params)]
    with CheckpointedFunction {

  private var worker_id: Int = -1

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

  /** The training data set buffer */
  private val training_set: mutable.Queue[LabeledVector] = mutable.Queue[LabeledVector]()
  private var c_training_set: ListState[LabeledVector] = _

  /** The test set buffer */
  private var test_set: ListBuffer[LabeledVector] = ListBuffer[LabeledVector]()
  private var c_test_set: ListState[LabeledVector] = _

  /** The local and last global learning parameters */
  private var model: l_params = _
  private var c_model: ListState[l_params] = _
  private var global_model: l_params = _
  private var c_global_model: ListState[l_params] = _
  Random.setSeed(25)

  val c: Double = 0.01

  override def flatMap(input: LearningMessage, out: Collector[(Int, Int, l_params)]): Unit = {
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

        //        if (Random.nextFloat() > 0.8) {
        if (count >= 8) {

          test_set += data
          if (test_set.length > 1000) training_set.enqueue(test_set.remove(0))

        } else {

          // Data point trigger functionality
          if (process_data && training_set.isEmpty) {
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
    count += 1
    if (count == 10) count = 0

    if (process_data) {
      while (processed_data < batch_size && training_set.nonEmpty) {
        fit(training_set.dequeue)
        processed_data += 1
      }

      if (checkIfMessageToServerIsNeeded()) sendModelToServer(out)
      //      if (training_set.isEmpty) println(worker_id)
    }

    //    accuracy(worker_id)
  }

  def fit(data: LabeledVector): Unit = {
    val label = if (data.label == 0.0) -1.0 else data.label
    val parameters: lin_params = model.asInstanceOf[lin_params]
    val loss: Double = 1.0 - label * ((data.vector.asBreeze dot parameters.weights) + parameters.intercept)

    if (loss > 0.0) {
      val Lagrange_Multiplier: Double = loss / (((data.vector dot data.vector) + 1.0) + 1 / (2 * c))
      model = lin_params(parameters.weights + Lagrange_Multiplier * label * data.vector.asBreeze,
        parameters.intercept + Lagrange_Multiplier * label)
    }
  }

  private def accuracy(partition: Int): Unit = {
    try {
      if (Random.nextFloat() >= 0.95 && model != null) {
        val accuracy: Double = (for (test <- test_set)
          yield {
            val prediction = if ((test.vector.asBreeze dot model.asInstanceOf[lin_params].weights)
              + model.asInstanceOf[lin_params].intercept >= 0.0) 1.0 else 0.0
            if (test.label == prediction) 1 else 0
          }).sum / (1.0 * test_set.length)

        println(partition, accuracy, training_set.length)
      }
    } catch {
      case _: Throwable => println(s"$partition can't produce score")
    }
  }

  private def updateLocalModel(data: l_params): Unit = {
    global_model = data
    model = data
  }

  private def init_model(data: LabeledVector): l_params = {
    lin_params(BreezeDenseVector.zeros[Double](data.vector.size), 0.0)
  }

  private def sendModelToServer(out: Collector[(Int, Int, l_params)]): Unit = {
    processed_data = 0
    process_data = false

    val mdl: l_params = {
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

  override def snapshotState(context: FunctionSnapshotContext): Unit = {
    if (model != null) {
      c_model.clear()
      c_model.add(model)
    }

    if (global_model != null) {
      c_global_model.clear()
      c_global_model.add(global_model)
    }

    if (test_set != null) {
      c_test_set.clear()
      for (i <- test_set.indices) c_test_set add test_set(i)
    }

    if (training_set != null) {
      c_training_set.clear()
      for (i <- training_set.indices) c_training_set add training_set.get(i).get
    }
  }

  override def initializeState(context: FunctionInitializationContext): Unit = {
    c_training_set = context.getOperatorStateStore.getListState(
      new ListStateDescriptor[LabeledVector]("training_set",
        createTypeInformation[LabeledVector])
    )

    c_test_set = context.getOperatorStateStore.getListState(
      new ListStateDescriptor[LabeledVector]("test_set",
        createTypeInformation[LabeledVector])
    )

    c_model = context.getOperatorStateStore.getListState(
      new ListStateDescriptor[l_params]("model",
        createTypeInformation[l_params])
    )

    c_global_model = context.getOperatorStateStore.getListState(
      new ListStateDescriptor[l_params]("global_model",
        TypeInformation.of(new TypeHint[l_params]() {}))
    )

    if (context.isRestored) {
      val it_m = c_model.get.iterator
      if (it_m.hasNext) model = it_m.next

      val it_gm = c_global_model.get.iterator
      if (it_gm.hasNext) global_model = it_gm.next

      test_set.clear
      val it_test = c_test_set.get.iterator
      while (it_test.hasNext) test_set += it_test.next

      training_set.clear
      val it_train = c_training_set.get.iterator
      while (it_train.hasNext) training_set enqueue it_train.next
    }

  }
}
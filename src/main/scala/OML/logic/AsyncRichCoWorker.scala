package OML.logic

import OML.common.{DataListAccumulator, DataQueueAccumulator, DataSetListAccumulator, DataSetQueueAccumulator, ParameterAccumulator, modelAccumulator}
import OML.learners.Learner
import OML.math.Point
import OML.message.{ControlMessage, DataPoint, workerMessage}
import OML.nodes.WorkerNode.RichCoWorkerLogic
import OML.parameters.{LearningParameters => l_params}
import org.apache.flink.api.common.state.{AggregatingState, AggregatingStateDescriptor, ValueState, ValueStateDescriptor}
import org.apache.flink.api.java.utils.ParameterTool
import org.apache.flink.configuration.Configuration
import org.apache.flink.util.Collector
import org.apache.flink.streaming.api.scala.createTypeInformation

import scala.collection.mutable
import scala.util.Random

class AsyncRichCoWorker[L <: Learner : Manifest]
  extends RichCoWorkerLogic[DataPoint, ControlMessage, workerMessage, L] {

  /** The id of the current worker/slave */
  private var worker_id: ValueState[Int] = _

  /** Used to sample data points for testing the accuracy of the model */
  private var count: ValueState[Int] = _

  /** Total number of fitted data points at the current worker */
  private var processed_data: ValueState[Int] = _

  /** A flag determining if the learner is allowed to fit new data.
    * When this is false, it means that the learner is waiting to
    * receive the new parameters from the coordinator
    */
  private var process_data: ValueState[Boolean] = _

  /** The number of data points fitted by the worker before
    * pushing the delta updates to the coordinator
    */
  private val batch_size: Int = 256

  /** The capacity of the data point buffer used for testing the performance
    * of the local model. This is done to prevent overflow */
  private val test_buffer_size: Int = 1000

  /** The capacity of the data point buffer used for training
    * the local model. This is done to prevent overflow */
  private val train_buffer_size: Int = 500000

  /** The training data set buffer */
  private var training_set: AggregatingState[Point, Option[Point]] = _
  private val training_set_size: mutable.Map[Int, Int] = mutable.Map[Int, Int]()

  /** The test set buffer */
  private var test_set: AggregatingState[Point, Option[Point]] = _
  private val test_set_size: mutable.Map[Int, Int] = mutable.Map[Int, Int]()

  /** The local and last global learning parameters */
  private implicit var model: AggregatingState[l_params, l_params] = _
  private var global_model: AggregatingState[l_params, l_params] = _

  /** The parallelism of the parameter server */
  private var psParallelism: ValueState[Int] = _

  Random.setSeed(25)

  override def flatMap1(input: DataPoint, out: Collector[workerMessage]): Unit = {
    input match {
      case DataPoint(partition, data) =>

        // Initializations
        try {
          require(partition == worker_id.value, s"message partition $partition integer does not equal worker ID $worker_id")
        } catch {
          case e: Exception =>
            if (worker_id.value < 0) {
              setWorkerId(partition)
              if (partition == 0) {
                learner.initialize_model_safe(data)
                process_data.update(true)
              }
            } else {
              throw new IllegalArgumentException(e.getMessage)
            }
        }

        // Train or test point
        if (count.value >= 8) {
          test_set add data
          if (test_set_size(worker_id.value) + 1 > test_buffer_size) {
            training_set add test_set.get.get
            overflowCheck(worker_id.value)
          } else {
            test_set_size(worker_id.value) = test_set_size(worker_id.value) + 1
          }
        } else {
          // Data point trigger functionality
          if (process_data.value && training_set_size(worker_id.value) == 0) {
            learner.fit_safe(data)
            processed_data.update(processed_data.value + 1)
          } else {
            training_set add data
            overflowCheck(worker_id.value)
          }
        }

        count.update(count.value + 1)
        if (count.value == 10) count.update(0)
        process(out)
    }
  }

  override def flatMap2(input: ControlMessage, out: Collector[workerMessage]): Unit = {
    input match {
      case ControlMessage(partition, data) =>
        try {
          require(partition == worker_id.value, s"message partition integer $partition does not equal worker ID $worker_id")
        } catch {
          case e: Exception =>
            if (worker_id.value < 0) {
              setWorkerId(partition)
            } else {
              throw new IllegalArgumentException(e.getMessage)
            }
        }
        updateLocalModel(data)
        process_data.update(true)
    }
    process(out)
  }

  private def process(out: Collector[workerMessage]): Unit = {
    if (process_data.value) {
      while (processed_data.value < batch_size && training_set_size(worker_id.value) > 0) {
        val batch_len: Int = Math.min(batch_size - processed_data.value, training_set_size(worker_id.value))
        for (_ <- 0 until batch_len) {
          learner.fit_safe(training_set.get.get)
          training_set_size(worker_id.value) = training_set_size(worker_id.value) - 1
        }
        processed_data.update(processed_data.value + batch_len)
      }

      if (checkIfMessageToServerIsNeeded()) sendModelToServer(out)
      //      if (training_set.isEmpty) println(worker_id)
    }

    if (Random.nextFloat() >= 0.96) {
      println(s"${worker_id.value}, ${
        learner.score_safe(test_set, test_set_size(worker_id.value)) match {
          case Some(score) => score
          case None => "Can't calculate score"
        }
      }, ${training_set_size(worker_id.value)}, ${test_set_size(worker_id.value)}")
    }
  }

  override def updateLocalModel(data: l_params): Unit = {
    model.clear()
    global_model.clear()
    model add data
    global_model add data.getCopy
  }

  override def sendModelToServer(out: Collector[workerMessage]): Unit = {
    processed_data.update(0)

    val mdl: l_params = {
      try {
        model.get - global_model.get
      } catch {
        case _: Throwable => model.get
      } finally {
        process_data.update(false)
      }
    }

    out.collect(workerMessage(0, worker_id.value, mdl))
  }

  override def setWorkerId(id: Int): Unit = {
    worker_id.update(id)
    training_set_size += (id -> 0)
    test_set_size += (id -> 0)
  }

  override def checkIfMessageToServerIsNeeded(): Boolean = processed_data.value >= batch_size

  private def overflowCheck(id: Int): Unit = {
    if (training_set_size(id) + 1 > train_buffer_size) {
      training_set.get
    } else {
      training_set_size(id) = training_set_size(id) + 1
    }
  }

  override def open(parameters: Configuration): Unit = {
    worker_id = getRuntimeContext.getState(
      new ValueStateDescriptor[Int]("worker_id", createTypeInformation[Int], -1))

    count = getRuntimeContext.getState(
      new ValueStateDescriptor[Int]("count", createTypeInformation[Int], 0))

    processed_data = getRuntimeContext.getState(
      new ValueStateDescriptor[Int]("processed_data", createTypeInformation[Int], 0))

    process_data = getRuntimeContext.getState(
      new ValueStateDescriptor[Boolean]("process_data", createTypeInformation[Boolean], false))

    training_set = getRuntimeContext.getAggregatingState[Point, DataQueueAccumulator, Option[Point]](
      new AggregatingStateDescriptor[Point, DataQueueAccumulator, Option[Point]](
        "training_set",
        new DataSetQueueAccumulator,
        createTypeInformation[DataQueueAccumulator]))

    test_set = getRuntimeContext.getAggregatingState[Point, DataListAccumulator, Option[Point]](
      new AggregatingStateDescriptor[Point, DataListAccumulator, Option[Point]](
        "test_set",
        new DataSetListAccumulator,
        createTypeInformation[DataListAccumulator]))

    model = getRuntimeContext.getAggregatingState[l_params, ParameterAccumulator, l_params](
      new AggregatingStateDescriptor[l_params, ParameterAccumulator, l_params](
        "model",
        new modelAccumulator,
        createTypeInformation[ParameterAccumulator]))

    global_model = getRuntimeContext.getAggregatingState[l_params, ParameterAccumulator, l_params](
      new AggregatingStateDescriptor[l_params, ParameterAccumulator, l_params](
        "global_model",
        new modelAccumulator,
        createTypeInformation[ParameterAccumulator]))

    psParallelism = getRuntimeContext.getState(
      new ValueStateDescriptor[Int]("psParallelism",
        createTypeInformation[Int],
        getRuntimeContext
          .getExecutionConfig
          .getGlobalJobParameters
          .asInstanceOf[ParameterTool]
          .get("psp", "1")
          .toInt))

  }

}
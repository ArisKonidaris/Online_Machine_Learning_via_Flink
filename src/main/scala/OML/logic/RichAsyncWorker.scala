package OML.logic

import OML.common.{DataQueueAccumulator, DataSetAccumulator, ParameterAccumulator, Point, modelAccumulator}
import OML.learners.Learner
import OML.message.{DataPoint, LearningMessage, psMessage}
import OML.nodes.WorkerNode.RichWorkerLogic
import OML.parameters.{LearningParameters => l_params}
import org.apache.flink.api.common.state.{AggregatingState, AggregatingStateDescriptor, ValueState, ValueStateDescriptor}
import org.apache.flink.configuration.Configuration
import org.apache.flink.streaming.api.scala.createTypeInformation
import org.apache.flink.util.Collector

import scala.collection.mutable
import scala.collection.mutable.ListBuffer

class RichAsyncWorker[L <: Learner : Manifest]
  extends RichWorkerLogic[LearningMessage, (Int, Int, l_params), L] {

  private var worker_id: ValueState[Int] = _

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

  /** The training data set buffer */
  private var training_set: AggregatingState[Point, Option[Point]] = _
  private var training_set_size: mutable.Map[Int, Int] = mutable.Map[Int, Int]()

  /** The test set buffer */
  private var test_set: mutable.Map[Int, ListBuffer[Point]] = mutable.Map[Int, ListBuffer[Point]]()

  implicit var model: AggregatingState[l_params, l_params] = _
  private var global_model: AggregatingState[l_params, l_params] = _

  override def flatMap(input: LearningMessage, out: Collector[(Int, Int, l_params)]): Unit = {
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

        if (count.value >= 8) {

          test_set(partition) += data
          if (test_set(partition).length > 1000) {
            training_set add test_set(partition).remove(0)
            training_set_size(input.partition) = training_set_size(input.partition) + 1
          }

        } else {

          // Data point trigger functionality
          if (process_data.value && training_set_size(partition) == 0) {
            learner.fit_safe(data)
            processed_data.update(processed_data.value + 1)
          } else {
            training_set add data
            training_set_size(partition) = training_set_size(partition) + 1
          }

        }

      case psMessage(partition, data) =>

        try {
          require(partition == worker_id.value, s"message partition $partition integer does not equal worker ID $worker_id")
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

    count.update(count.value + 1)
    if (count.value == 10) count.update(0)

    if (process_data.value) {
      while (training_set_size(input.partition) > 0 && processed_data.value < batch_size) {
        training_set.get match {
          case Some(dataPoint: Point) =>
            learner.fit_safe(dataPoint)
            training_set_size(input.partition) = training_set_size(input.partition) - 1
        }
        processed_data.update(processed_data.value + 1)
      }

      if (checkIfMessageToServerIsNeeded()) sendModelToServer(out)
      //      if (training_set_size(input.partition) == 0) println(worker_id.value)
    }

    //    if (Random.nextFloat() >= 0.95)
    //      println(s"${worker_id.value}, ${
    //        learner.score_safe(test_set(input.partition)) match {
    //          case Some(acc) => acc
    //          case None => "Can't calculate score"
    //        }
    //      }, ${training_set_size(input.partition)}, ${test_set(worker_id.value).length}")
  }

  override def updateLocalModel(data: l_params): Unit = {
    model.clear()
    global_model.clear()
    model add data
    global_model add data
  }

  override def checkIfMessageToServerIsNeeded(): Boolean = processed_data.value == batch_size

  override def sendModelToServer(out: Collector[(Int, Int, l_params)]): Unit = {
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

    out.collect((0, worker_id.value, mdl))
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
        new DataSetAccumulator,
        createTypeInformation[DataQueueAccumulator]))

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
  }

  override def setWorkerId(partition: Int): Unit = {
    worker_id.update(partition)
    test_set += (partition -> ListBuffer[Point]())
    training_set_size += (partition -> 0)
  }

}

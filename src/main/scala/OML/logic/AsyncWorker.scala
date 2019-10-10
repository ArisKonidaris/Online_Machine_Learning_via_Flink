package OML.logic

import OML.common.Point
import OML.learners.Learner
import OML.message.{DataPoint, LearningMessage, psMessage}
import OML.nodes.WorkerNode.WorkerLogic
import OML.parameters.{LearningParameters => l_params}
import org.apache.flink.api.common.state.{ListState, ListStateDescriptor}
import org.apache.flink.api.common.typeinfo.{TypeHint, TypeInformation}
import org.apache.flink.runtime.state.{FunctionInitializationContext, FunctionSnapshotContext}
import org.apache.flink.streaming.api.scala.createTypeInformation
import org.apache.flink.util.Collector

import scala.collection.mutable
import scala.collection.mutable.ListBuffer
import scala.util.Random

class AsyncWorker[L <: Learner : Manifest]
  extends WorkerLogic[LearningMessage, (Int, Int, l_params), L] {

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
  private val training_set: mutable.Queue[Point] = mutable.Queue[Point]()
  private var c_training_set: ListState[Point] = _

  /** The test set buffer */
  private var test_set: ListBuffer[Point] = ListBuffer[Point]()
  private var c_test_set: ListState[Point] = _

  /** The local and last global learning parameters */
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
                learner.initialize_model(data)
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
            learner.fit(data)
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
        learner.fit(training_set.dequeue())
        processed_data += 1
      }

      if (checkIfMessageToServerIsNeeded()) sendModelToServer(out)
      //      if (training_set.isEmpty) println(worker_id)
    }

    //    if(Random.nextFloat() >= 0.95) {
    //      println(s"$worker_id, ${
    //        learner.score(test_set) match {
    //          case Some(score) => score
    //          case None => "Can't calculate score"
    //        }
    //      }, ${training_set.length}, ${test_set.length}")
    //    }

  }

  override def updateLocalModel(params: l_params): Unit = {
    global_model = params
    learner.set_params(params)
  }

  override def sendModelToServer(out: Collector[(Int, Int, l_params)]): Unit = {
    processed_data = 0
    process_data = false

    val mdl: l_params = {
      try {
        learner.get_params() - global_model
      } catch {
        case _: Throwable => learner.get_params()
      }
    }

    out.collect((0, worker_id, mdl))
  }

  override def setWorkerId(id: Int): Unit = worker_id = id

  override def checkIfMessageToServerIsNeeded(): Boolean = processed_data == batch_size

  override def snapshotState(context: FunctionSnapshotContext): Unit = {
    if (learner.get_params() != null) {
      c_model.clear()
      c_model.add(learner.get_params())
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
      new ListStateDescriptor[Point]("training_set",
        createTypeInformation[Point])
    )

    c_test_set = context.getOperatorStateStore.getListState(
      new ListStateDescriptor[Point]("test_set",
        createTypeInformation[Point])
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
      if (it_m.hasNext) learner.set_params(it_m.next)

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
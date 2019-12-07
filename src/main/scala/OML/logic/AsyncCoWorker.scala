package OML.logic

import OML.learners.Learner
import OML.math.Point
import OML.message.{ControlMessage, DataPoint, workerMessage}
import OML.nodes.WorkerNode.CoWorkerLogic
import OML.parameters.{LinearModelParameters, LearningParameters => l_params}
import OML.pipeline.Pipeline
import OML.preprocessing.{PolynomialFeatures, StandardScaler}
import org.apache.flink.api.common.state.{ListState, ListStateDescriptor}
import org.apache.flink.api.common.typeinfo.{TypeHint, TypeInformation}
import org.apache.flink.runtime.state.{FunctionInitializationContext, FunctionSnapshotContext}
import org.apache.flink.streaming.api.scala.createTypeInformation
import org.apache.flink.util.Collector

import scala.collection.mutable.ListBuffer
import scala.util.Random

/** A CoFlatMap modelling a worker in the online
  * asynchronous distributed Machine Learning protocol.
  *
  * @tparam L The machine learning algorithm
  */
class AsyncCoWorker[L <: Learner : Manifest]
  extends CoWorkerLogic[DataPoint, ControlMessage, workerMessage, L] {

  /** The id of the current worker/slave */
  private var worker_id: Int = -1

  /** A flag enabled when two workers are merged by Flink due to a node failure */
  private var merged: Boolean = false

  /** Used to sample data points for testing the accuracy of the model */
  private var count: Int = 0

  /** Total number of fitted data points at the current worker */
  private var processed_data: Int = 0
  private var c_processed_data: ListState[Int] = _

  /** A flag determining if the learner is allowed to fit new data.
    * When this is false, it means that the learner is waiting to
    * receive the new parameters from the coordinator
    */
  private var process_data: Boolean = false
  private var c_process_data: ListState[Boolean] = _

  /** The number of data points fitted by the worker before
    * pushing the delta updates to the coordinator
    */
  private val batch_size: Int = 256

  /** The capacity of the data point buffer used for testing the performance
    * of the local model. This is done to prevent overflow */
  private val test_set_max_size: Int = 500

  /** The capacity of the data point buffer used for training
    * the local model. This is done to prevent overflow */
  private val train_set_max_size: Int = 500000

  /** The training data set buffer */
  private val training_set: ListBuffer[Point] = ListBuffer[Point]()
  private var c_training_set: ListState[Point] = _

  /** The test set buffer */
  private var test_set: ListBuffer[Point] = ListBuffer[Point]()
  private var c_test_set: ListState[Point] = _

  /** The local and last global learning parameters */
  private var c_model: ListState[l_params] = _
  private var global_model: l_params = _
  private var c_global_model: ListState[l_params] = _

  /** An ML pipeline test */
  private val pipeline: Pipeline = Pipeline()
    .addPreprocessor(new PolynomialFeatures)
    .addLearner(learner)

  Random.setSeed(25)

  /** The flatMap of the fitting phase of the learners.
    *
    * The new data point is either fitted directly to the learner, buffered if
    * the worker waits for the response of the parameter server or used as a
    * test point for testing the performance of the model.
    *
    * @param input A data point for training
    * @param out   The flatMap collector
    */
  override def flatMap1(input: DataPoint, out: Collector[workerMessage]): Unit = {
    input match {
      case DataPoint(partition, data) =>

        // Initializations
        try {
          require(partition == worker_id, s"message partition $partition integer does not equal worker ID $worker_id")
        } catch {
          case e: Exception =>
            if (worker_id < 0) {
              setWorkerId(partition)
              if (merged) {
                out.collect(new workerMessage())
                merged = false
              } else {
                if (partition == 0 && pipeline.getLearner.get_params() == null) {
                  pipeline.init(data)
                  process_data = true
                }
              }
            } else {
              throw new IllegalArgumentException(e.getMessage)
            }
        }

        // Train or test point
        if (count >= 8) {
          test_set += data
          if (test_set.length > test_set_max_size) {
            training_set += test_set.remove(0)
            overflowCheck()
          }
        } else {
          if (process_data && training_set.isEmpty) {
            pipeline.fit(data)
            processed_data += 1
          } else {
            training_set += data
            overflowCheck()
          }
        }
    }

    count += 1
    if (count == 10) count = 0
    process(out)

  }

  /** The flatMap of the control stream.
    *
    * The control stream are the parameter server messages
    * and the User's control mechanisms.
    *
    * @param input The control message
    * @param out   The flatMap collector
    */
  override def flatMap2(input: ControlMessage, out: Collector[workerMessage]): Unit = {
    input match {
      case ControlMessage(partition, data) =>
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
    process(out)
  }

  /** A bulk fitting operation.
    *
    * @param out The flatMap collector
    */
  private def process(out: Collector[workerMessage]): Unit = {
    if (process_data) {
      while (processed_data < batch_size && training_set.nonEmpty) {
        val batch_len: Int = Math.min(batch_size - processed_data, training_set.length)
        pipeline.fit(training_set.slice(0, batch_len))
        training_set.remove(0, batch_len)
        processed_data += batch_len
      }

      if (checkIfMessageToServerIsNeeded()) sendModelToServer(out)
      //      if (training_set.isEmpty) println(worker_id)
    }

    if (Random.nextFloat() >= 0.98) {
      println(s"$worker_id, ${
        pipeline.score(test_set) match {
          case Some(score) => score
          case None => "Can't calculate score"
        }
      }, ${training_set.length}, ${test_set.length}")
    }
  }

  /** The response of the parameter server with the new global parameters.
    *
    * @param global_params The global parameters
    */
  override def updateLocalModel(global_params: l_params): Unit = {
    global_model = global_params
    pipeline.getLearner.set_params(global_model.getCopy)
  }

  /** Method for pushing the local parameter updates to the parameter server.
    *
    * @param out The flatMap collector
    */
  override def sendModelToServer(out: Collector[workerMessage]): Unit = {
    processed_data = 0
    process_data = false

    val mdl: l_params = {
      try {
        pipeline.getLearner.get_params() - global_model
      } catch {
        case _: Throwable => pipeline.getLearner.get_params()
      }
    }

    out.collect(workerMessage(0, worker_id, mdl, 1))
  }

  /** A setter method for the id of local worker.
    *
    * The worker_id is sent to he parameter server, so that
    * Flink can partitions its answer to the correct worker.
    *
    * */
  override def setWorkerId(id: Int): Unit = worker_id = id

  /** Method determining if the worker needs to pull the global
    * parameters from the parameter server.
    *
    * For the default asynchronous distributed ML, the worker pulls the
    * parameters periodically, after the fitting of a constant number of data points.
    *
    * @return Whether to request the global parameters from the parameter server
    */
  override def checkIfMessageToServerIsNeeded(): Boolean = processed_data >= batch_size

  /** Method that prevents memory overhead due to the data point buffer.
    *
    * The worker cannot train on any data while waiting for the response of the parameter
    * server with the new global model, so it cashes any new data point in that time. This
    * method monitors the size of that buffer. If the buffer becomes too large, the oldest
    * data point is discarded to prevent memory overhead.
    *
    */
  private def overflowCheck(): Unit = {
    if (training_set.length > train_set_max_size) training_set.remove(Random.nextInt(train_set_max_size + 1))
  }

  /** Snapshot operation.
    *
    * Takes a snapshot of the operator when
    * a checkpoint has to be performed.
    *
    * @param context Flink's FunctionSnapshotContext
    */
  override def snapshotState(context: FunctionSnapshotContext): Unit = {

    // =================================== Snapshot the local model =================================

    if (pipeline.getLearner.get_params() != null) {
      c_model.clear()
      c_model.add(pipeline.getLearner.get_params())
    }

    // ========================= Snapshot the last global model of the worker =======================

    if (global_model != null) {
      c_global_model.clear()
      c_global_model.add(global_model)
    }

    // =================================== Snapshot the test set ====================================

    if (test_set != null) {
      c_test_set.clear()
      for (i <- test_set.indices) c_test_set add test_set(i)
    }

    // =================================== Snapshot the training set ================================

    if (training_set != null) {
      c_training_set.clear()
      for (i <- training_set.indices) c_training_set add training_set(i)
    }

    // =================================== Snapshot flags and counters ===============================

    c_process_data.clear()
    c_process_data add process_data

    c_processed_data.clear()
    c_processed_data add processed_data

  }


  /** Operator initializer method.
    *
    * Is called every time the user-defined function is initialized,
    * be that when the function is first initialized or be that when
    * the function is actually recovering from an earlier checkpoint.
    *
    * @param context Flink's FunctionSnapshotContext
    */
  override def initializeState(context: FunctionInitializationContext): Unit = {

    c_process_data = context.getOperatorStateStore.getListState(
      new ListStateDescriptor[Boolean]("process_data",
        createTypeInformation[Boolean])
    )

    c_processed_data = context.getOperatorStateStore.getListState(
      new ListStateDescriptor[Int]("processed_data",
        createTypeInformation[Int])
    )

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

    // =================================== Restart strategy ===========================================

    if (context.isRestored) {

      // =================================== Restoring flags and counters =============================

      val it_f = c_process_data.get.iterator
      if (it_f.hasNext) process_data = it_f.next

      val it_p = c_processed_data.get.iterator
      if (it_p.hasNext) processed_data = it_p.next

      // =================================== Restoring the training set ===============================

      training_set.clear
      val it_train = c_training_set.get.iterator
      while (it_train.hasNext) {
        training_set += it_train.next
        overflowCheck()
      }

      // =================================== Restoring the test set ===================================

      test_set.clear
      val it_test = c_test_set.get.iterator
      while (it_test.hasNext) {
        if (test_set.length < test_set_max_size) {
          test_set += it_test.next
        } else {
          training_set += it_test.next
          overflowCheck()
        }
      }

      // =================================== Restoring the global model ================================

      val it_gm = c_global_model.get.iterator
      if (it_gm.hasNext) global_model = it_gm.next

      // =================================== Restoring the model =======================================

      var count = 0
      var mdl: l_params = null
      val it_m = c_model.get.iterator
      while (it_m.hasNext) {
        if (mdl == null) {
          mdl = it_m.next
        } else {
          mdl += it_m.next
        }
        count += 1
      }
      if (count > 1) {
        mdl /= count
        process_data = false
        processed_data = 0
        merged = true
      }
      pipeline.getLearner.set_params(mdl)

    }

  }
}
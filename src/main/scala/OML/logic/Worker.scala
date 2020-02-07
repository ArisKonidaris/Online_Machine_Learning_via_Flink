package OML.logic

import OML.StarProtocolAPI.Node
import OML.common.OMLTools._
import OML.math.Point
import OML.message.packages._
import OML.message.{ControlMessage, DataPoint, workerMessage}
import OML.mlAPI.MLWorker
import OML.mlAPI.dprotocol.PeriodicMLWorker
import OML.nodes.site.SiteLogic
import OML.parameters.{LearningParameters => l_params}
import org.apache.flink.api.common.state.{ListState, ListStateDescriptor}
import org.apache.flink.api.common.typeinfo.{TypeHint, TypeInformation}
import org.apache.flink.runtime.state.{FunctionInitializationContext, FunctionSnapshotContext}
import org.apache.flink.util.Collector

import scala.collection.mutable
import scala.collection.mutable.ListBuffer
import scala.util.Random

/** A CoFlatMap modelling a worker in the online
  * asynchronous distributed Machine Learning proto.
  *
  */
class Worker extends SiteLogic[DataPoint, ControlMessage, workerMessage] {

  /** The id of the current worker/slave */
  private var worker_id: Int = -1

  /** A flag enabled when two workers are merged by Flink due to a node failure */
  private var merged: Boolean = false

  /** Used to sample data points for testing the accuracy of the model */
  private var count: Int = 0

  /** The capacity of the data point buffer used for testing the performance
    * of the local model. This is done to prevent overflow */
  private val test_set_max_size: Int = 500

  /** The test set buffer */
  private var test_set: ListBuffer[Point] = ListBuffer[Point]()
  private var c_test_set: ListState[ListBuffer[Point]] = _

  /** An ML pipeline test */
  private var ml_workers: ListState[scala.collection.mutable.Map[Int, MLWorker]] = _

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
              setSiteID(partition)
              for ((key, ml_worker) <- state) {
                ml_worker.setID(worker_id.toString + "_" + key)
                if (merged) {
                  out.collect(new workerMessage(key, worker_id))
                  merged = false
                } else {
                  ml_worker.getLearnerParams match {
                    case None =>
                      if (partition == 0)
                        ml_worker.init(data)
                      else
                        out.collect(new workerMessage(key, worker_id))
                    case _ =>
                  }
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
            val point: Point = test_set.remove(0)
            for ((_, ml_worker: MLWorker) <- state) ml_worker.getTrainingSet.append(point)
          }
        } else for ((_, ml_worker: MLWorker) <- state) ml_worker.processPoint(data)

      case _ => throw new Exception("Unrecognized tuple type")
    }

    count += 1
    if (count == 10) count = 0
    sendToCoordinator(out)

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
      case ControlMessage(request, workerID, pipelineID, data, cont) =>
        request match {
          case UpdatePipelinePS =>
            try {
              require(workerID == worker_id, s"message partition integer $workerID does not equal worker ID $worker_id")
            } catch {
              case e: Exception =>
                if (worker_id < 0) {
                  setSiteID(workerID)
                } else {
                  throw new IllegalArgumentException(e.getMessage)
                }
            }
            updateState(pipelineID, data.get)
            sendToCoordinator(out)

          case CreatePipeline =>
            if (!state.contains(pipelineID)) {
              state += (pipelineID -> MLWorker().configureWorker(cont.get.asInstanceOf[MLPipelineContainer]))
              if (worker_id >= 0) state(pipelineID).setID(worker_id.toString + "_" + pipelineID)
            }

          case UpdatePipeline => println(input)
          case DeletePipeline => println(input)
          case _ => println("Not recognized request")
        }
    }
  }

  /** Snapshot operation.
    *
    * Takes a snapshot of the operator when
    * a checkpoint has to be performed.
    *
    * @param context Flink's FunctionSnapshotContext
    */
  override def snapshotState(context: FunctionSnapshotContext): Unit = {

    // =================================== Snapshot the test set ====================================

    if (test_set != null) {
      c_test_set.clear()
      c_test_set add test_set
    }

    // =================================== Snapshot the test set ====================================
    ml_workers.clear()
    ml_workers add state

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

    ml_workers = context.getOperatorStateStore.getListState(
      new ListStateDescriptor[scala.collection.mutable.Map[Int, MLWorker]]("MLWorker",
        TypeInformation.of(new TypeHint[scala.collection.mutable.Map[Int, MLWorker]]() {}))
    )

    // =================================== Restart strategy ===========================================

    if (context.isRestored) {

      var count: Int = 0

      // =================================== Restoring the ML workers =================================

      state.clear()
      val it_pip = ml_workers.get.iterator
      if (it_pip.hasNext) state = it_pip.next
      while (it_pip.hasNext) {
        val tmpPipe: mutable.Map[Int, MLWorker] = it_pip.next
        for ((key, ml_worker) <- state) ml_worker.merge(tmpPipe(key))
        count += 1
      }
      for ((_, ml_worker) <- state) ml_worker.getTrainingSet.completeMerge()
      if (count > 1) merged = true

      // =================================== Restoring the test set ===================================

      test_set.clear
      count = 1
      val it_test = c_test_set.get.iterator
      if (it_test.hasNext) {
        test_set = it_test.next
        while (it_test.hasNext) {
          val next: ListBuffer[Point] = it_test.next
          if (next.nonEmpty) {
            if (test_set.isEmpty) {
              test_set = next
            } else {
              test_set = mergeBufferedPoints(1, test_set.length, 0, next.length, test_set, next, count)
            }
          }
          count += 1
        }
        while (test_set.length > test_set_max_size) {
          val point: Point = test_set.remove(0)
          for ((_, ml_worker) <- state) ml_worker.getTrainingSet.append(point)
        }
      }

    }

  }

  /** Method for pushing the local parameter updates to the parameter server.
    *
    * @param out The flatMap collector
    */
  override def sendToCoordinator(out: Collector[workerMessage]): Unit = {
    for ((_, ml_worker: MLWorker) <- state) {
      val msgQ: mutable.Queue[workerMessage] = ml_worker.getMessageQueue
      while (msgQ.nonEmpty) out.collect(msgQ.dequeue())
    }
    if (Random.nextFloat() >= 0.995) checkScore()
  }

  /** Print the score of each ML Worker for the local test set for debugging */
  private def checkScore(): Unit = {
    for ((_, ml_worker: MLWorker) <- state)
      println(s"$worker_id, ${ml_worker.scoreVerbose(test_set)}")
  }

  /** The response of the parameter server with the new global parameters
    * of an ML pipeline
    *
    * @param stateID       The ML pipeline identifier
    * @param global_params The global parameters
    */
  override def updateState(stateID: Int, global_params: l_params): Unit = state(stateID).updateModel(global_params)

  /** A setter method for the id of local worker.
    *
    * The worker_id is sent to he parameter server, so that
    * Flink can partitions its answer to the correct worker.
    *
    */
  override def setSiteID(siteID: Int): Unit = worker_id = siteID

}
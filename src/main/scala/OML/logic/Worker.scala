package OML.logic

import OML.common.OMLTools._
import OML.math.Point
import OML.message.packages._
import OML.message.{ControlMessage, DataPoint, workerMessage}
import OML.mlAPI.pipeline.Pipeline
import OML.nodes.WorkerNode.CoWorkerLogic
import OML.parameters.{LearningParameters => l_params}
import org.apache.flink.api.common.state.{ListState, ListStateDescriptor}
import org.apache.flink.api.common.typeinfo.{TypeHint, TypeInformation}
import org.apache.flink.runtime.state.{FunctionInitializationContext, FunctionSnapshotContext}
import org.apache.flink.util.Collector

import scala.collection.mutable
import scala.collection.mutable.ListBuffer
import scala.util.Random

/** A CoFlatMap modelling a worker in the online
  * asynchronous distributed Machine Learning protocol.
  *
  */
class Worker extends CoWorkerLogic[DataPoint, ControlMessage, workerMessage] {

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

  private var ml_pipeline: ListState[scala.collection.mutable.Map[Int, Pipeline]] = _

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
              for ((key, pipeline) <- pipelines) {
                pipeline.setID(worker_id.toString + "_" + key)
                if (merged) {
                  out.collect(new workerMessage(key, worker_id))
                  merged = false
                } else {
                  pipeline.getLearnerParams match {
                    case None =>
                      if (partition == 0)
                        pipeline.init(data)
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
            for ((_, pipeline: Pipeline) <- pipelines) pipeline.appendToTrainSet(point)
          }
        } else for ((_, pipeline: Pipeline) <- pipelines) pipeline.processPoint(data)

      case _ => throw new Exception("Unrecognized tuple type")
    }

    count += 1
    if (count == 10) count = 0
    sendModelToServer(out)

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
                  setWorkerId(workerID)
                } else {
                  throw new IllegalArgumentException(e.getMessage)
                }
            }
            updatePipeline(pipelineID, data.get)
            sendModelToServer(out)

          case CreatePipeline =>
            if (!pipelines.contains(pipelineID))
              pipelines += (pipelineID -> Pipeline().configurePipeline(cont.get.asInstanceOf[PipelineContainer]))

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
    ml_pipeline.clear()
    ml_pipeline add pipelines

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

    ml_pipeline = context.getOperatorStateStore.getListState(
      new ListStateDescriptor[scala.collection.mutable.Map[Int, Pipeline]]("ml_pipeline",
        TypeInformation.of(new TypeHint[scala.collection.mutable.Map[Int, Pipeline]]() {}))
    )

    // =================================== Restart strategy ===========================================

    if (context.isRestored) {

      var count: Int = 0

      // =================================== Restoring the model =======================================

      pipelines.clear()
      val it_pip = ml_pipeline.get.iterator
      if (it_pip.hasNext) pipelines = it_pip.next
      while (it_pip.hasNext) {
        val tmpPipe: mutable.Map[Int, Pipeline] = it_pip.next
        for ((key, pipeline) <- pipelines) pipeline.merge(tmpPipe(key))
        count += 1
      }
      for ((_, pipeline) <- pipelines) pipeline.completeMerge()
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
          for ((_, pipeline) <- pipelines) pipeline.appendToTrainSet(point)
        }
      }

    }

  }

  /** Method for pushing the local parameter updates to the parameter server.
    *
    * @param out The flatMap collector
    */
  override def sendModelToServer(out: Collector[workerMessage]): Unit = {
    for ((_, pipeline: Pipeline) <- pipelines) {
      val msgQ: mutable.Queue[Serializable] = pipeline.getMessageQueue
      while (msgQ.nonEmpty) out.collect(msgQ.dequeue().asInstanceOf[workerMessage])
    }

    if (Random.nextFloat() >= 0.995) checkScore()
  }

  /** Print the score of each Pipeline for the local test set for debugging */
  private def checkScore(): Unit = {
    for ((_, pipeline: Pipeline) <- pipelines)
      println(s"$worker_id, ${pipeline.scoreVerbose(test_set)}")
  }

  /** The response of the parameter server with the new global hyperparameters
    * of an ML pipeline
    *
    * @param pID           The pipeline identifier
    * @param global_params The global hyperparameters
    */
  override def updatePipeline(pID: Int, global_params: l_params): Unit = pipelines(pID).updateModel(global_params)

  /** A setter method for the id of local worker.
    *
    * The worker_id is sent to he parameter server, so that
    * Flink can partitions its answer to the correct worker.
    *
    * */
  override def setWorkerId(workerID: Int): Unit = worker_id = workerID

}
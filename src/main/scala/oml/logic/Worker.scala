package oml.logic

import oml.StarProtocolAPI.{Node, NodeGenerator}
import oml.common.OMLTools._
import oml.math.Point
import oml.message.{ControlMessage, DataPoint, workerMessage}
import oml.nodes.site.SiteLogic
import org.apache.flink.api.common.state.{ListState, ListStateDescriptor}
import org.apache.flink.api.common.typeinfo.{TypeHint, TypeInformation}
import org.apache.flink.runtime.state.{FunctionInitializationContext, FunctionSnapshotContext}
import org.apache.flink.util.Collector

import scala.collection.mutable
import scala.collection.mutable.ListBuffer
import scala.reflect.Manifest
import scala.util.Random

/** A CoFlatMap Flink Function modelling a worker in a star distributed topology.
  */
class Worker[G <: NodeGenerator](implicit man: Manifest[G])
  extends SiteLogic[DataPoint, ControlMessage, workerMessage] {

  /** The id of the current worker/slave */
  private var worker_id: Int = -1

  /** Used to sample data points for testing the accuracy of the model */
  private var count: Int = 0

  /** The capacity of the data point buffer used for testing the performance
    * of the local model. This is done to prevent overflow */
  private val test_set_max_size: Int = 500

  /** The test set buffer */
  // TODO: Make the test_set something like a Node object.
  private var test_set: ListBuffer[Point] = ListBuffer[Point]()
  private var c_test_set: ListState[ListBuffer[Point]] = _

  /** An ML pipeline test */
  private var nodes: ListState[scala.collection.mutable.Map[Int, Node]] = _

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

        checkId(partition)

        // Train or test point
        if (count >= 8) {
          test_set += data
          if (test_set.length > test_set_max_size) {
            val data: Point = test_set.remove(0)
            for ((_, node: Node) <- state) node.receiveTuple(data)
          }
        } else for ((_, node: Node) <- state) node.receiveTuple(data)

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
      case ControlMessage(request, workerID, pipelineID, data, conf) =>
        checkId(workerID)
        request match {
          case -3 =>
            if (state.contains(pipelineID)) state.remove(pipelineID)
          case _ =>
            if (!state.contains(pipelineID))
              state += (pipelineID -> NodeFactory.generate(
                conf.get.addParameter("id", worker_id.toString + "_" + pipelineID)))
            state(pipelineID).receiveMsg(request, data.get)
            sendToCoordinator(out)
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
    nodes.clear()
    nodes add state

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

    nodes = context.getOperatorStateStore.getListState(
      new ListStateDescriptor[scala.collection.mutable.Map[Int, Node]]("node",
        TypeInformation.of(new TypeHint[scala.collection.mutable.Map[Int, Node]]() {}))
    )

    // =================================== Restart strategy ===========================================

    if (context.isRestored) {


      // =================================== Restoring the ML workers =================================

      state.clear()
      val it_pip = nodes.get.iterator
      if (it_pip.hasNext) state = it_pip.next
      while (it_pip.hasNext) {
        val tmpPipe: mutable.Map[Int, Node] = it_pip.next
        for ((key, node) <- state) node.merge(tmpPipe(key))
      }

      // =================================== Restoring the test set ===================================

      test_set.clear
      var cnt: Int = 1
      val it_test = c_test_set.get.iterator
      if (it_test.hasNext) {
        test_set = it_test.next
        while (it_test.hasNext) {
          val next: ListBuffer[Point] = it_test.next
          if (next.nonEmpty)
            if (test_set.isEmpty) {
              test_set = next
            } else {
              test_set = mergeBufferedPoints(1, test_set.length, 0, next.length, test_set, next, cnt)
              cnt += 1
            }
        }
        while (test_set.length > test_set_max_size) {
          val point: Point = test_set.remove(0)
          for ((_, node) <- state) node.receiveTuple(point)
        }
      }

    }

  }

  /** Method for pushing the local parameter updates to the parameter server.
    *
    * @param out The flatMap collector
    */
  def sendToCoordinator(out: Collector[workerMessage]): Unit = {
    // TODO: Change this. This code will change after the proxy implementation.
    for ((_, node: Node) <- state) {
      node.send(out)
    }
    if (Random.nextFloat() >= 0.995) checkScore()
  }

  /** Print the score of each ML Worker for the local test set for debugging */
  private def checkScore(): Unit = {
    // TODO: Change this. You should not bind any operation to a specific Int.
    for ((_, node: Node) <- state) node.receiveMsg(3, test_set)
  }

  /** A setter method for the id of local worker.
    *
    * The worker_id is sent to he parameter server, so that
    * Flink can partitions its answer to the correct worker.
    *
    */
  override def setSiteID(siteID: Int): Unit = worker_id = siteID

  private def checkId(id: Int): Unit = {
    try {
      require(id == worker_id, s"message partition $id integer does not equal worker ID $worker_id")
    } catch {
      case _: Exception => setSiteID(id)
    }
  }

  private def NodeFactory: NodeGenerator = man.runtimeClass.newInstance().asInstanceOf[NodeGenerator]

}
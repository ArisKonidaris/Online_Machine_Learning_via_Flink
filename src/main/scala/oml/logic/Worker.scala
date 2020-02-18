package oml.logic

import java.io.Serializable

import oml.StarProtocolAPI._
import oml.message.mtypes.{ControlMessage, DataPoint, workerMessage}
import oml.mlAPI.dataBuffers.DataSet
import oml.nodes.site.SiteLogic
import org.apache.flink.api.common.state.{ListState, ListStateDescriptor}
import org.apache.flink.api.common.typeinfo.{TypeHint, TypeInformation}
import org.apache.flink.runtime.state.{FunctionInitializationContext, FunctionSnapshotContext}
import org.apache.flink.util.Collector

import scala.collection.mutable
import scala.reflect.Manifest
import scala.util.Random

/** A CoFlatMap Flink Function modelling a worker in a star distributed topology.
  */
class Worker[G <: WorkerGenerator](implicit man: Manifest[G])
  extends SiteLogic[DataPoint, ControlMessage, workerMessage]
    with Network {

  /** The id of the current worker/slave */
  private var worker_id: Int = -1

  /** Used to sample data points for testing the accuracy of the model */
  private var count: Int = 0

  /** The test set buffer */
  // TODO: Make the test_set something like a Node object.
  private var test_set: DataSet = new DataSet(500)
  private var shared_data: ListState[DataSet] = _

  /** An ML pipeline test */
  private var nodes: ListState[scala.collection.mutable.Map[Int, GenericWrapper]] = _

  private var collector: Collector[workerMessage] = _

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
        collector = out

        // Train or test point
        if (count >= 8) {
          test_set.append(data) match {
            case None =>
            case Some(point: Serializable) =>
              for ((_, node: Node) <- state) node.receiveTuple(Array[AnyRef](point))
          }
        } else
          for ((_, node: Node) <- state) node.receiveTuple(Array[AnyRef](data))


      case _ => throw new Exception("Unrecognized tuple type")
    }

    count += 1
    if (count == 10) count = 0
    checkScore()
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
      case ControlMessage(request, workerID, nodeID, data, config) =>
        checkId(workerID)
        collector = out

        request match {
          case -3 =>
            if (state.contains(nodeID)) state.remove(nodeID)
          case 0 =>
            if (!state.contains(nodeID) && config.isDefined)
              state += (nodeID -> new FlinkWrapper(nodeID,
                nodeFactory,
                config.get.addParameter("id", workerID),
                this))
          case _ =>
            if (state.contains(nodeID)) {
              state(nodeID).receiveMsg(request, Array[AnyRef](data.get))
              checkScore()
            }
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
      shared_data.clear()
      shared_data add test_set
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
      new ListStateDescriptor[scala.collection.mutable.Map[Int, GenericWrapper]]("node",
        TypeInformation.of(new TypeHint[scala.collection.mutable.Map[Int, GenericWrapper]]() {}))
    )

    shared_data = context.getOperatorStateStore.getListState(
      new ListStateDescriptor[DataSet]("shared_data",
        TypeInformation.of(new TypeHint[DataSet]() {}))
    )

    // =================================== Restart strategy ===========================================

    if (context.isRestored) {


      // =================================== Restoring the ML workers =================================

      state.clear()
      val it_pip = nodes.get.iterator
      if (it_pip.hasNext) state = it_pip.next
      while (it_pip.hasNext) {
        val tmpPipe: mutable.Map[Int, GenericWrapper] = it_pip.next
        for ((key, node) <- state) node.merge(tmpPipe(key))
      }

      // =================================== Restoring the test set ===================================

      test_set.clear()
      val it_test = shared_data.get.iterator
      if (it_test.hasNext) {
        test_set = it_test.next
        while (it_test.hasNext) {
          val next: DataSet = it_test.next
          if (next.nonEmpty) if (test_set.isEmpty) test_set = next else test_set = test_set.merge(next)
        }
        test_set.completeMerge()
        while (test_set.length > test_set.getMaxSize)
          for ((_, node) <- state) node.receiveTuple(test_set.remove(0).get)
      }

    }

  }

  /** Print the score of each ML Worker for the local test set for debugging */
  private def checkScore(): Unit = {
    // TODO: Change this. You should not bind any operation to a specific Int.
    if (Random.nextFloat() >= 0.996)
      for ((_, node: Node) <- state) node.receiveMsg(2, Array[AnyRef](test_set.data_buffer))
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

  private def nodeFactory: WorkerGenerator = man.runtimeClass.newInstance().asInstanceOf[WorkerGenerator]

  override def send(destination: Integer, operation: Integer, message: Serializable): Boolean = {
    collector.collect(workerMessage(destination, worker_id, message, operation))
    true
  }

  override def broadcast(operation: Integer, message: Serializable): Boolean = true

  override def describe(): Int = 0
}
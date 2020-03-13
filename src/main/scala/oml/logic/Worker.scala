package oml.logic

import java.io.Serializable

import oml.FlinkBackend.POJOs.Request
import oml.FlinkBackend.wrappers.FlinkWrapper
import oml.StarTopologyAPI._
import oml.StarTopologyAPI.network.{Network, Node}
import oml.math.Point
import oml.message.mtypes.{ControlMessage, DataPoint, workerMessage}
import oml.mlAPI.dataBuffers.DataSet
import oml.nodes.site.SiteLogic
import org.apache.flink.api.common.state.{ListState, ListStateDescriptor}
import org.apache.flink.api.common.typeinfo.{TypeHint, TypeInformation}
import org.apache.flink.runtime.state.{FunctionInitializationContext, FunctionSnapshotContext}
import org.apache.flink.util.Collector

import scala.collection.mutable
import scala.collection.mutable.ListBuffer
import scala.reflect.Manifest
import scala.util.Random
import scala.collection.JavaConverters._

/** A CoFlatMap Flink Function modelling a worker request a star distributed topology.
  */
class Worker[G <: WorkerGenerator](implicit man: Manifest[G])
  extends SiteLogic[DataPoint, ControlMessage, workerMessage]
    with Network {

  /** Used to sample data points for testing the accuracy of the model */
  private var count: Int = 0

  /** The test set buffer */
  // TODO: Make the test_set something like a Node object.
  private var test_set: DataSet[Point] = new DataSet[Point](500)
  private var saved_test_set: ListState[DataSet[Point]] = _

  /** An ML pipeline test */
  private var nodes: ListState[scala.collection.mutable.Map[Int, GenericWrapper]] = _
  private var saved_cache: ListState[DataSet[Point]] = _

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
      case DataPoint(data) =>
        collector = out
        if (state.nonEmpty) {
          if (cache.nonEmpty) {
            cache.append(data)
            while (cache.nonEmpty) {
              val point = cache.pop().get
              handleData(point)
            }
          } else handleData(data)
        } else cache.append(data)

      case _ => throw new Exception("Unrecognized tuple type")
    }

    checkScore()
  }

  private def handleData(data: Serializable): Unit = {
    // Train or test point
    if (count >= 8)
      test_set.append(data.asInstanceOf[Point]) match {
        case None =>
        case Some(point: Serializable) =>
          for ((_, node: Node) <- state) node.receiveTuple(Array[Any](point))
      }
    else
      for ((_, node: Node) <- state) node.receiveTuple(Array[Any](data))
    count += 1
    if (count == 10) count = 0
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
      case ControlMessage(operation, workerID, nodeID, data, request) =>
        checkId(workerID)
        collector = out

        operation match {
          case Some(op: Int) =>
            if (state.contains(nodeID)) {
              state(nodeID).receiveMsg(op, Array[AnyRef](data.get))
              checkScore()
            }
          case None =>
            request match {
              case None => println(s"Empty request in worker ${getRuntimeContext.getIndexOfThisSubtask}.")
              case Some(request: Request) =>
                request.getRequest match {
                  case "Create" =>
                    if (!state.contains(nodeID)) {
                      var config: mutable.Map[String, AnyRef] = request.getTraining_configuration.asScala
                      if (config == null) config = new mutable.HashMap[String, AnyRef]()
                      config.put("FlinkWorkerID", getRuntimeContext.getIndexOfThisSubtask.asInstanceOf[AnyRef])
                      request.setTraining_configuration(config.asJava)
                      state += (nodeID -> new FlinkWrapper(nodeID,
                        nodeFactory,
                        request,
                        this))
                    }

                  case "Update" =>
                  case "Query" =>
                  case "Delete" => if (state.contains(nodeID)) state.remove(nodeID)
                  case _: String =>
                    println(s"Invalid request type in worker ${getRuntimeContext.getIndexOfThisSubtask}.")
                }
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
      saved_test_set.clear()
      saved_test_set add test_set
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

    saved_test_set = context.getOperatorStateStore.getListState(
      new ListStateDescriptor[DataSet[Point]]("saved_test_set",
        TypeInformation.of(new TypeHint[DataSet[Point]]() {}))
    )

    saved_cache = context.getOperatorStateStore.getListState(
      new ListStateDescriptor[DataSet[Point]]("saved_cache",
        TypeInformation.of(new TypeHint[DataSet[Point]]() {}))
    )

    // =================================== Restart strategy ===========================================

    if (context.isRestored) {

      // =================================== Restoring the ML workers =================================
      state.clear()
      val it_pip = nodes.get.iterator
      if (it_pip.hasNext) {
        state = it_pip.next
        while (it_pip.hasNext) {
          val tmpState: mutable.Map[Int, GenericWrapper] = it_pip.next
          assert(state.size == tmpState.size)
          for ((key, node) <- state) node.merge(tmpState(key))
        }
      }

      // =================================== Restoring the test set ===================================
      test_set.clear()
      test_set = mergingDataBuffers(saved_test_set)
      if (test_set.length > test_set.getMaxSize) {
        test_set.completeMerge() match {
          case Some(extraData: ListBuffer[_]) =>
            for (data <- extraData) for ((_, node) <- state) node.receiveTuple(data)
          case None =>
        }
      }

      // =================================== Restoring the data cache ==================================
      cache.clear()
      cache = mergingDataBuffers(saved_cache)
      cache.completeMerge()

    }

  }

  private def mergingDataBuffers(saved_buffers: ListState[DataSet[Point]]): DataSet[Point] = {
    var new_buffer = new DataSet[Point]()

    val buffers_iterator = saved_buffers.get.iterator
    while (buffers_iterator.hasNext) {
      val next = buffers_iterator.next
      if (next.nonEmpty) if (new_buffer.isEmpty) new_buffer = next else new_buffer = new_buffer.merge(next)
    }
    new_buffer.completeMerge()

    while (new_buffer.length > new_buffer.getMaxSize) {
      if (state.nonEmpty)
        for ((_, node) <- state) node.receiveTuple(new_buffer.remove(0).get)
      else
        new_buffer.remove(0).get
    }

    new_buffer
  }


  /** Print the score of each ML Worker for the local test set for debugging */
  private def checkScore(): Unit = {
    // TODO: Change this. You should not bind any operation to a specific Int.
    if (Random.nextFloat() >= 0.996)
      for ((_, node: Node) <- state) node.receiveMsg(2, Array[AnyRef](test_set.data_buffer))
  }

  private def checkId(id: Int): Unit = {
    try {
      require(id == getRuntimeContext.getIndexOfThisSubtask,
        s"Worker ID is not equal to the Index of the Flink Subtask")
    } catch {
      case e: Exception => e.printStackTrace()
    }
  }

  private def nodeFactory: WorkerGenerator = man.runtimeClass.newInstance().asInstanceOf[WorkerGenerator]

  override def send(destination: Integer, operation: Integer, message: Serializable): Boolean = {
    collector.collect(workerMessage(destination, getRuntimeContext.getIndexOfThisSubtask, message, operation))
    true
  }

  override def broadcast(operation: Integer, message: Serializable): Unit = {

  }

  override def describe(): Int = 0
}
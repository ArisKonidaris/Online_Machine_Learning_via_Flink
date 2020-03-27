package oml.logic

import java.io.Serializable

import oml.OML_Job.queryResponse
import oml.POJOs.{QueryResponse, Request}
import oml.StarTopologyAPI._
import oml.math.Point
import oml.message.mtypes.{ControlMessage, workerMessage}
import oml.mlAPI.dataBuffers.DataSet
import oml.nodes.site.SiteLogic
import org.apache.flink.api.common.state.{ListState, ListStateDescriptor}
import org.apache.flink.api.common.typeinfo.{TypeHint, TypeInformation}
import org.apache.flink.runtime.state.{FunctionInitializationContext, FunctionSnapshotContext}
import org.apache.flink.streaming.api.functions.co.CoProcessFunction
import org.apache.flink.util.Collector

import scala.collection.mutable
import scala.reflect.Manifest
import scala.util.Random
import scala.collection.JavaConverters._
import scala.collection.mutable.ListBuffer

/** A CoFlatMap Flink Function modelling a workers request a star distributed topology. */
class Trainer[G <: WorkerGenerator](implicit man: Manifest[G])
  extends SiteLogic[Point, ControlMessage, workerMessage]
    with Network {

  /** Used to sample data points for testing the accuracy of the model */
  protected var count: Int = 0

  /** The test set buffer */
  // TODO: Make the test_set something like a Node object.
  protected var test_set: DataSet[Point] = new DataSet[Point](500)
  protected var saved_test_set: ListState[DataSet[Point]] = _

  /** An ML pipeline test */
  protected var nodes: ListState[scala.collection.mutable.Map[Int, GenericWrapper]] = _
  protected var saved_cache: ListState[DataSet[Point]] = _

  protected var collector: Collector[workerMessage] = _
  protected var context: CoProcessFunction[Point, ControlMessage, workerMessage]#Context = _

  Random.setSeed(25)

  /** The process for the fitting phase of the learners.
    *
    * The new data point is either fitted directly to the learner, buffered if
    * the workers waits for the response of the parameter server or used as a
    * test point for testing the performance of the model.
    *
    * @param data A data point for training
    * @param out  The process collector
    */
  override def processElement1(data: Point,
                               ctx: CoProcessFunction[Point, ControlMessage, workerMessage]#Context,
                               out: Collector[workerMessage]): Unit = {
    collector = out
    context = ctx
    if (state.nonEmpty) {
      if (cache.nonEmpty) {
        cache.append(data)
        while (cache.nonEmpty) {
          val point = cache.pop().get
          handleData(point)
        }
      } else handleData(data)
    } else cache.append(data)
  }

  def handleData(data: Serializable): Unit = {
    // Train or test point
    if (getRuntimeContext.getIndexOfThisSubtask == 0) {
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
    } else {
      if (test_set.nonEmpty())
        while (test_set.nonEmpty()) {
          val point = test_set.pop().get
          for ((_, node: Node) <- state) node.receiveTuple(Array[Any](point))
        }
      else
        for ((_, node: Node) <- state) node.receiveTuple(Array[Any](data))
    }
  }

  /** The process function of the control stream.
    *
    * The control stream are the parameter server messages
    * and the User's control mechanisms.
    *
    * @param message The control message
    * @param out     The process function collector
    */
  def processElement2(message: ControlMessage,
                      ctx: CoProcessFunction[Point, ControlMessage, workerMessage]#Context,
                      out: Collector[workerMessage]): Unit = {
    message match {
      case ControlMessage(operation, workerID, nodeID, data, request) =>
        checkId(workerID)
        collector = out
        context = ctx

        operation match {
          case Some(op: Int) =>
            if (state.contains(nodeID)) state(nodeID).receiveMsg(op, Array[AnyRef](data.get))

          case None =>
            request match {
              case None => println(s"Empty request in workers ${getRuntimeContext.getIndexOfThisSubtask}.")
              case Some(request: Request) =>
                request.getRequest match {
                  case "Create" =>
                    if (!state.contains(nodeID)) {
                      var config: mutable.Map[String, AnyRef] = request.getTraining_configuration.asScala
                      if (config == null) config = new mutable.HashMap[String, AnyRef]()
                      config.put("FlinkWorkerID", getRuntimeContext.getIndexOfThisSubtask.asInstanceOf[AnyRef])
                      request.setTraining_configuration(config.asJava)
                      state += (nodeID -> new FlinkWrapper(nodeID,
                        nodeFactory.generateTrainingWorker(request),
                        this))
                    }

                  case "Update" =>

                  case "Query" =>
                    if (state.contains(nodeID) && request.getRequestId != null)
                      state(nodeID).query(request.getRequestId, test_set.data_buffer.toArray)

                  case "Delete" => if (state.contains(nodeID)) state.remove(nodeID)

                  case _: String =>
                    println(s"Invalid request type in workers ${getRuntimeContext.getIndexOfThisSubtask}.")
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

      var new_state = scala.collection.mutable.Map[Int, GenericWrapper]()
      val it_pip = nodes.get.iterator
      val remoteStates = ListBuffer[scala.collection.mutable.Map[Int, GenericWrapper]]()
      while (it_pip.hasNext) {
        val next = it_pip.next
        if (next.nonEmpty)
          if (new_state.isEmpty)
            new_state = next
          else {
            assert(new_state.size == next.size)
            remoteStates.append(next)
          }
      }

      for ((key, node) <- state) node.merge((for (remoteState <- remoteStates) yield remoteState(key)).toArray)


      // =================================== Restoring the test set ===================================

      test_set.clear()
      test_set = mergingDataBuffers(saved_test_set)
      if (state.nonEmpty)
        while (test_set.length > test_set.getMaxSize)
          for ((_, node) <- state)
            node.receiveTuple(test_set.pop().get)
      else
        while (test_set.length > test_set.getMaxSize)
          test_set.pop()
      assert(test_set.length <= test_set.getMaxSize)

      // =================================== Restoring the data cache ==================================

      cache.clear()
      cache = mergingDataBuffers(saved_cache)
      if (state.nonEmpty)
        while (cache.nonEmpty)
          handleData(cache.pop().get)
      else
        while (cache.length > cache.getMaxSize)
          cache.pop()
      assert(cache.length <= cache.getMaxSize)
    }

  }

  def checkId(id: Int): Unit = {
    try {
      require(id == getRuntimeContext.getIndexOfThisSubtask,
        s"Trainer ID is not equal to the Index of the Flink Subtask")
    } catch {
      case e: Exception => e.printStackTrace()
    }
  }

  override def send(destination: Integer, operation: Integer, message: Serializable): Unit = {
    message match {
      case array: Array[AnyRef] =>
        assert(!array.isEmpty)
        array(0) match {
          case response: QueryResponse => context.output(queryResponse, response)
          case _ =>
            collector.collect(workerMessage(destination, getRuntimeContext.getIndexOfThisSubtask, message, operation))
        }
      case _ =>
        collector.collect(workerMessage(destination, getRuntimeContext.getIndexOfThisSubtask, message, operation))
    }
  }

  override def broadcast(operation: Integer, message: Serializable): Unit = {

  }

  override def describe(): Unit = {

  }

  def nodeFactory: WorkerGenerator = man.runtimeClass.newInstance().asInstanceOf[WorkerGenerator]

}
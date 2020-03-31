package oml.FlinkBipartiteAPI.operators

import java.io.Serializable

import oml.mlAPI.math.Point
import oml.FlinkBipartiteAPI.POJOs.Request
import oml.StarTopologyAPI.{BufferingWrapper, NodeGenerator}
import oml.StarTopologyAPI.network.Node
import oml.FlinkBipartiteAPI.messages.{ControlMessage, SpokeMessage}
import oml.FlinkBipartiteAPI.network.FlinkNetwork
import oml.mlAPI.dataBuffers.DataSet
import oml.FlinkBipartiteAPI.nodes.spoke.SpokeLogic
import oml.StarTopologyAPI.operations.RemoteCallIdentifier
import oml.StarTopologyAPI.sites.{NodeId, NodeType}
import org.apache.flink.api.common.state.{ListState, ListStateDescriptor}
import org.apache.flink.api.common.typeinfo.{TypeHint, TypeInformation}
import org.apache.flink.runtime.state.{FunctionInitializationContext, FunctionSnapshotContext}
import org.apache.flink.streaming.api.functions.co.CoProcessFunction
import org.apache.flink.util.Collector

import scala.collection.mutable.ListBuffer
import scala.reflect.Manifest
import scala.util.Random

/** A CoFlatMap Flink Function modelling a worker request a star distributed topology.
  */
class FlinkSpoke[G <: NodeGenerator](implicit man: Manifest[G])
  extends SpokeLogic[Point, ControlMessage, SpokeMessage] {

  /** Used to sample data points for testing the score of the model. */
  private var count: Int = 0

  /** The test set buffer. */
  private var test_set: DataSet[Point] = new DataSet[Point](500)
  private var saved_test_set: ListState[DataSet[Point]] = _

  /** The nodes trained in the current Flink operator instance. */
  private var nodes: ListState[scala.collection.mutable.Map[Int, BufferingWrapper[Point]]] = _
  private var saved_cache: ListState[DataSet[Point]] = _

  private var collector: Collector[SpokeMessage] = _
  private var context: CoProcessFunction[Point, ControlMessage, SpokeMessage]#Context = _

  Random.setSeed(25)

  /** The process for the fitting phase of the learners.
    *
    * The new data point is either fitted directly to the learner, buffered if
    * the workers waits for the response of the parameter server or used as a
    * test point for testing the performance of the model.
    *
    * @param data A data point for training.
    * @param out  The process collector.
    */
  override def processElement1(data: Point,
                               ctx: CoProcessFunction[Point, ControlMessage, SpokeMessage]#Context,
                               out: Collector[SpokeMessage]): Unit = {
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

  private def handleData(data: Serializable): Unit = {
    // Train or test point.
    if (getRuntimeContext.getIndexOfThisSubtask == 0) {
      if (count >= 8) {
        test_set.append(data.asInstanceOf[Point]) match {
          case Some(point: Serializable) => for ((_, node: Node) <- state) node.receiveTuple(Array[Any](point))
          case None =>
        }
      } else for ((_, node: Node) <- state) node.receiveTuple(Array[Any](data))
      count += 1
      if (count == 10) count = 0
    } else {
      if (test_set.nonEmpty()) {
        test_set.append(data.asInstanceOf[Point]) match {
          case Some(point: Serializable) => for ((_, node: Node) <- state) node.receiveTuple(Array[Any](point))
          case None =>
        }
        while (test_set.nonEmpty()) {
          val point = test_set.pop().get
          for ((_, node: Node) <- state) node.receiveTuple(Array[Any](point))
        }
      } else for ((_, node: Node) <- state) node.receiveTuple(Array[Any](data))
    }
    checkScore()
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
                      ctx: CoProcessFunction[Point, ControlMessage, SpokeMessage]#Context,
                      out: Collector[SpokeMessage]): Unit = {
    message match {
      case ControlMessage(network, operation, source, destination, data, request) =>
        checkId(destination.getNodeId)
        collector = out
        context = ctx

        operation match {
          case rpc: RemoteCallIdentifier =>
            if (state.contains(network)) state(network).receiveMsg(source, rpc, Array[Any](data))
            checkScore()

          case null =>
            request match {
              case null => println(s"Empty request in worker ${getRuntimeContext.getIndexOfThisSubtask}.")
              case request: Request =>
                request.getRequest match {
                  case "Create" =>
                    if (!state.contains(network)) {
                      val hubParallelism: Int = {
                        if (request.getTraining_configuration.containsKey("HubParallelism"))
                          request.getTraining_configuration.get("HubParallelism").asInstanceOf[Int]
                        else
                          1
                      }
                      val flinkNetwork = FlinkNetwork[Point, ControlMessage, SpokeMessage](
                        NodeType.SPOKE,
                        network,
                        getRuntimeContext.getExecutionConfig.getParallelism,
                        hubParallelism)
                      flinkNetwork.setCollector(collector)
                      flinkNetwork.setContext(context)
                      state += (
                        network -> new BufferingWrapper(
                          new NodeId(NodeType.SPOKE, getRuntimeContext.getIndexOfThisSubtask),
                          nodeFactory.generateSpokeNode(request),
                          flinkNetwork,
                          new DataSet[Point]())
                        )
                      for (i <- 0 until hubParallelism)
                        out.collect(SpokeMessage(network, null, null, new NodeId(NodeType.HUB, i), null, request))
                    }

                  case "Update" =>

                  case "Query" =>
                    if (request.getRequestId != null)
                      if (state.contains(network))
                        state(network).receiveQuery(request.getRequestId, test_set.data_buffer.toArray)
                      else
                        println("No such Network.")
                    else println("No requestId given for the query.")

                  case "Delete" => if (state.contains(network)) state.remove(network)

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

    // ======================================= Snapshot the test set ===================================================

    if (test_set != null) {
      saved_test_set.clear()
      saved_test_set add test_set
    }

    // ==================================== Snapshot the network nodes =================================================
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
      new ListStateDescriptor[scala.collection.mutable.Map[Int, BufferingWrapper[Point]]]("node",
        TypeInformation.of(new TypeHint[scala.collection.mutable.Map[Int, BufferingWrapper[Point]]]() {}))
    )

    saved_test_set = context.getOperatorStateStore.getListState(
      new ListStateDescriptor[DataSet[Point]]("saved_test_set",
        TypeInformation.of(new TypeHint[DataSet[Point]]() {}))
    )

    saved_cache = context.getOperatorStateStore.getListState(
      new ListStateDescriptor[DataSet[Point]]("saved_cache",
        TypeInformation.of(new TypeHint[DataSet[Point]]() {}))
    )

    // ============================================ Restart strategy ===================================================

    if (context.isRestored) {

      // ======================================== Restoring the Spokes =================================================

      state.clear()

      var new_state = scala.collection.mutable.Map[Int, BufferingWrapper[Point]]()
      val it_pip = nodes.get.iterator
      val remoteStates = ListBuffer[scala.collection.mutable.Map[Int, BufferingWrapper[Point]]]()
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

      // ====================================== Restoring the test set =================================================

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

      // ====================================== Restoring the data cache ===============================================

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


  /** Print the score of each ML FlinkSpoke for the local test set for debugging */
  private def checkScore(): Unit = {
    // TODO: Change this. You should not bind any operation to a specific Int.
    if (Random.nextFloat() >= 0.996)
      for ((_, node: Node) <- state) node.receiveMsg(null, null, Array[AnyRef](test_set.data_buffer))
  }

  private def checkId(id: Int): Unit = {
    try {
      require(id == getRuntimeContext.getIndexOfThisSubtask,
        s"FlinkSpoke ID is not equal to the Index of the Flink Subtask")
    } catch {
      case e: Exception => e.printStackTrace()
    }
  }

  private def nodeFactory: NodeGenerator = man.runtimeClass.newInstance().asInstanceOf[NodeGenerator]

}
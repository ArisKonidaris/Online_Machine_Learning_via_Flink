package oml.logic

import java.io.Serializable

import oml.POJOs.{DataInstance, Prediction, Request}
import oml.StarTopologyAPI._
import oml.message.mtypes.ControlMessage
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

class Predictor[G <: WorkerGenerator](implicit man: Manifest[G])
  extends SiteLogic[DataInstance, ControlMessage, Prediction]
    with Network {

  /** The ML pipelines */
  private var nodes: ListState[scala.collection.mutable.Map[Int, GenericWrapper]] = _
  private var savedCache: ListState[DataSet[DataInstance]] = _

  private var collector: Collector[Prediction] = _

  Random.setSeed(25)

  /** The process operation of the prediction.
    *
    * The new data point is either forwarded for prediction, or buffered
    * if the operator instance does not have any predictor pipelines.
    *
    * @param data A data point for prediction.
    * @param out  The process operation collector.
    */
  override def processElement1(data: DataInstance,
                               context: CoProcessFunction[DataInstance, ControlMessage, Prediction]#Context,
                               out: Collector[Prediction]): Unit = {
    collector = out
    if (state.nonEmpty) {
      if (cache.nonEmpty) {
        cache.append(data)
        while (cache.nonEmpty) {
          val point = cache.pop().get
          for ((_, node: Node) <- state) node.receiveTuple(Array[Any](point))
        }
      } else for ((_, node: Node) <- state) node.receiveTuple(Array[Any](data))
    } else cache.append(data)
  }

  /** The process function of the control stream.
    *
    * The control stream is responsible for updating the predictors.
    *
    * @param message The control message with a new model.
    * @param out     The process function collector.
    */
  override def processElement2(message: ControlMessage,
                               context: CoProcessFunction[DataInstance, ControlMessage, Prediction]#Context,
                               out: Collector[Prediction]): Unit = {
    message match {
      case ControlMessage(operation, workerID, nodeID, data, request) =>
        checkId(workerID)

        collector = out
        operation match {
          case Some(op: Int) =>
            if (state.contains(nodeID)) state(nodeID).receiveMsg(op, Array[AnyRef](data.get))
          case None =>
            request match {
              case None => println(s"Empty request in predictor ${getRuntimeContext.getIndexOfThisSubtask}.")
              case Some(request: Request) =>
                request.getRequest match {
                  case "Create" =>
                    if (!state.contains(nodeID)) {
                      var config: mutable.Map[String, AnyRef] = request.getTraining_configuration.asScala
                      if (config == null) config = new mutable.HashMap[String, AnyRef]()
                      config.put("FlinkWorkerID", getRuntimeContext.getIndexOfThisSubtask.asInstanceOf[AnyRef])
                      request.setTraining_configuration(config.asJava)
                      state += (nodeID -> new FlinkWrapper(nodeID,
                        nodeFactory.generatePredictionWorker(request),
                        this))
                    }
                  case "Update" =>
                  case "Query" =>
                  case "Delete" =>
                    if (state.contains(nodeID)) {
                      state.remove(nodeID)
                      println(s"Pipeline $nodeID has been removed " +
                        s"from predictor ${getRuntimeContext.getIndexOfThisSubtask}")
                    }
                  case _ => println(s"Invalid request type in predictor ${getRuntimeContext.getIndexOfThisSubtask}.")
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

    savedCache = context.getOperatorStateStore.getListState(
      new ListStateDescriptor[DataSet[DataInstance]]("savedCache",
        TypeInformation.of(new TypeHint[DataSet[DataInstance]]() {}))
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

      // =================================== Restoring the data cache ==================================
      cache.clear()
      cache = mergingDataBuffers(savedCache)
      if (state.nonEmpty)
        while (cache.length > cache.getMaxSize)
          for ((_, node) <- state)
            node.receiveTuple(cache.pop().get)
      else
        while (cache.length > cache.getMaxSize)
          cache.pop()
      assert(cache.length <= cache.getMaxSize)
    }

  }

  private def checkId(id: Int): Unit = {
    try {
      require(id == getRuntimeContext.getIndexOfThisSubtask,
        s"Trainer ID is not equal to the Index of the Flink Subtask")
    } catch {
      case e: Exception => e.printStackTrace()
    }
  }

  private def nodeFactory: WorkerGenerator = man.runtimeClass.newInstance().asInstanceOf[WorkerGenerator]

  override def send(destination: Integer, operation: Integer, message: Serializable): Unit = {
    collector.collect(message.asInstanceOf[Array[AnyRef]](0).asInstanceOf[Prediction])
  }

  override def broadcast(operation: Integer, message: Serializable): Unit = {

  }

  override def describe(): Unit = {

  }

}
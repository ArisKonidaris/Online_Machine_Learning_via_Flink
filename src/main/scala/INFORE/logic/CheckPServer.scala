package INFORE.logic

import INFORE.message.{LearningMessage, psMessage}
import org.apache.flink.api.common.functions.FlatMapFunction
import INFORE.parameters.{LearningParameters => l_params, LinearModelParameters => lin_params}
import org.apache.flink.api.common.state.{ListState, ListStateDescriptor}
import org.apache.flink.runtime.state.{FunctionInitializationContext, FunctionSnapshotContext}
import org.apache.flink.streaming.api.checkpoint.CheckpointedFunction
import org.apache.flink.streaming.api.scala.createTypeInformation
import org.apache.flink.util.Collector

class CheckPServer(var k: Int) extends FlatMapFunction[(Int, Int, l_params), LearningMessage] with CheckpointedFunction {

  private var global_model: l_params = _

  private var workers: ListState[Int] = _
  private var c_global_model: ListState[l_params] = _


  override def initializeState(context: FunctionInitializationContext): Unit = {
    workers = context.getOperatorStateStore.getListState(
      new ListStateDescriptor[Int]("workers",
        createTypeInformation[Int]))

    c_global_model = context.getOperatorStateStore.getListState(
      new ListStateDescriptor[l_params]("global_model",
        createTypeInformation[l_params])
    )

    if (context.isRestored) {
      val it_k = workers.get.iterator
      if (it_k.hasNext) k = it_k.next

      val it_m = c_global_model.get.iterator
      if (it_m.hasNext) global_model = it_m.next
    }

  }

  override def flatMap(in: (Int, Int, l_params), collector: Collector[LearningMessage]): Unit = {
    receiveMessage(in, collector)
  }

  private def receiveMessage(in: (Int, Int, l_params), collector: Collector[LearningMessage]): Unit = {
    try {
      updateGlobalModel(in._3)
    } catch {
      case _: Throwable => for (i <- 1 until k) sendMessage(i, collector)
    }
    finally {
      sendMessage(in._2, collector)
    }
  }

  private def updateGlobalModel(localModel: l_params): Unit = {
    try {
      global_model = global_model + (localModel * (1 / (1.0 * k)))
    } catch {
      case _: Throwable =>
        global_model = localModel
        throw new Exception
    }
  }

  private def sendMessage(id: Int, collector: Collector[LearningMessage]): Unit = {
    collector.collect(psMessage(id, global_model))
  }

  override def snapshotState(functionSnapshotContext: FunctionSnapshotContext): Unit = {
    workers.clear()
    workers add k
    if (global_model != null) {
      c_global_model.clear()
      c_global_model add global_model
    }
  }

}

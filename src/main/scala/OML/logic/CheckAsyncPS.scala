package OML.logic

import OML.message.{ControlMessage, workerMessage}
import OML.nodes.ParameterServerNode.PSLogic
import OML.parameters.{LearningParameters => l_params}
import org.apache.flink.api.common.state.{AggregatingState, ListState, ListStateDescriptor}
import org.apache.flink.streaming.api.scala.createTypeInformation
import org.apache.flink.runtime.state.{FunctionInitializationContext, FunctionSnapshotContext}
import org.apache.flink.util.Collector

class CheckAsyncPS(var k: Int) extends PSLogic[workerMessage, ControlMessage] {
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

  override def flatMap(in: workerMessage, collector: Collector[ControlMessage]): Unit = {
    receiveMessage(in, collector)
  }

  override def receiveMessage(in: workerMessage, collector: Collector[ControlMessage]): Unit = {
    try {
      updateGlobalModel(in.parameters)
    } catch {
      case _: Throwable => for (i <- 1 until k) sendMessage(i, collector)
    }
    finally {
      sendMessage(in.workerId, collector)
    }
  }

  override def updateGlobalModel(localModel: l_params): Unit = {
    try {
      global_model = global_model + (localModel * (1 / (1.0 * k)))
    } catch {
      case _: Throwable =>
        global_model = localModel
        throw new Exception
    }
  }

  override def sendMessage(id: Int, collector: Collector[ControlMessage]): Unit = {
    collector.collect(ControlMessage(id, global_model))
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

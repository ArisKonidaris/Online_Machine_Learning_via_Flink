package OML.logic

import OML.common.{Counter, IntegerAccumulator, ParameterAccumulator, modelAccumulator}
import OML.message.{ControlMessage, workerMessage}
import OML.nodes.ParameterServerNode.RichPSLogic
import OML.parameters.{LearningParameters => l_params}
import org.apache.flink.api.common.state.{AggregatingState, AggregatingStateDescriptor}
import org.apache.flink.configuration.Configuration
import org.apache.flink.streaming.api.scala.createTypeInformation
import org.apache.flink.util.Collector

class RichAsyncPS extends RichPSLogic[workerMessage, ControlMessage] {

  private implicit var global_model: AggregatingState[l_params, l_params] = _
  private var updates: AggregatingState[Int, Int] = _

  override def flatMap(in: workerMessage, collector: Collector[ControlMessage]): Unit = {
    receiveMessage(in, collector)
    updates add 1
  }

  override def open(parameters: Configuration): Unit = {

    updates = getRuntimeContext.getAggregatingState[Int, Counter, Int](
      new AggregatingStateDescriptor[Int, Counter, Int](
        "updates",
        new IntegerAccumulator,
        createTypeInformation[Counter]
      ))

    global_model = getRuntimeContext.getAggregatingState[l_params, ParameterAccumulator, l_params](
      new AggregatingStateDescriptor[l_params, ParameterAccumulator, l_params](
        "global_model",
        new modelAccumulator,
        createTypeInformation[ParameterAccumulator]))
  }

  override def receiveMessage(in: workerMessage, collector: Collector[ControlMessage]): Unit = {
    in.request match {
      case 0 =>
        // This handles a node failure. The restored node requests the global parameters
        sendMessage(in.workerId, collector)
      case 1 =>
        // This is the asynchronous push/pull
        updateGlobalModel(in.parameters)
        sendMessage(in.workerId, collector)
        if (in.workerId == 0 && updates.get == 0)
          for (i <- 1 until getRuntimeContext.getExecutionConfig.getParallelism)
            sendMessage(i, collector)
    }

  }

  override def updateGlobalModel(localModel: l_params): Unit = {
    global_model add (localModel * (1 / (1.0 * getRuntimeContext.getExecutionConfig.getParallelism)))
  }

  override def sendMessage(id: Int, collector: Collector[ControlMessage]): Unit = {
    collector.collect(ControlMessage(id, global_model.get))
  }

}

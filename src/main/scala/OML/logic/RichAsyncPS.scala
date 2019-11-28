package OML.logic

import OML.common.{Counter, IntegerAccumulator, ParameterAccumulator, modelAccumulator}
import OML.message.{ControlMessage, workerMessage}
import OML.nodes.ParameterServerNode.RichPSLogic
import OML.parameters.{LearningParameters => l_params}
import org.apache.flink.api.common.state.{AggregatingState, AggregatingStateDescriptor, ValueState, ValueStateDescriptor}
import org.apache.flink.api.java.utils.ParameterTool
import org.apache.flink.configuration.Configuration
import org.apache.flink.streaming.api.scala.createTypeInformation
import org.apache.flink.util.Collector

class RichAsyncPS extends RichPSLogic[workerMessage, ControlMessage] {

  private var workers: ValueState[Int] = _
  private implicit var global_model: AggregatingState[l_params, l_params] = _
  private var updates: AggregatingState[Int, Int] = _

  override def flatMap(in: workerMessage, collector: Collector[ControlMessage]): Unit = {
    receiveMessage(in, collector)
    updates add 1
  }

  override def open(parameters: Configuration): Unit = {
    workers = getRuntimeContext.getState(
      new ValueStateDescriptor[Int]("workers",
        createTypeInformation[Int],
        getRuntimeContext
          .getExecutionConfig
          .getGlobalJobParameters
          .asInstanceOf[ParameterTool]
          .getRequired("k")
          .toInt))

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
    //    require(getRuntimeContext.getExecutionConfig.getMaxParallelism == workers.value)
    updateGlobalModel(in.data)
    sendMessage(in.workerId, collector)
    if (in.workerId == 0 && updates.get == 0) for (i <- 1 until workers.value) sendMessage(i, collector)
  }

  override def updateGlobalModel(localModel: l_params): Unit = {
    global_model add (localModel * (1 / (1.0 * workers.value)))
  }

  override def sendMessage(id: Int, collector: Collector[ControlMessage]): Unit = {
    collector.collect(ControlMessage(id, global_model.get))
  }

}

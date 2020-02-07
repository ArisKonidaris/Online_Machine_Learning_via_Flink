package OML.logic

import OML.common.{ParameterAccumulator, modelAccumulator}
import OML.message.packages.UpdatePipelinePS
import OML.message.{ControlMessage, workerMessage}
import OML.nodes.hub.CoordinatorLogic
import OML.parameters.{LearningParameters => l_params}
import org.apache.flink.api.common.state.{ListState, ListStateDescriptor, ValueState, ValueStateDescriptor}
import org.apache.flink.api.common.state.{AggregatingState, AggregatingStateDescriptor}
import org.apache.flink.configuration.Configuration
import org.apache.flink.streaming.api.scala.createTypeInformation
import org.apache.flink.util.Collector

class ParameterServer extends CoordinatorLogic[workerMessage, ControlMessage] {

  private implicit var global_model: AggregatingState[l_params, l_params] = _
  private var pipeline_id: ValueState[Int] = _
  private var started: ValueState[Boolean] = _
  private var requests: ListState[Int] = _

  override def open(parameters: Configuration): Unit = {

    pipeline_id = getRuntimeContext.getState(
      new ValueStateDescriptor[Int]("pipeline_id", createTypeInformation[Int], -1))

    started = getRuntimeContext.getState(
      new ValueStateDescriptor[Boolean]("started", createTypeInformation[Boolean], false))

    requests = getRuntimeContext.getListState(
      new ListStateDescriptor[Int]("requests", createTypeInformation[Int]))

    global_model = getRuntimeContext.getAggregatingState[l_params, ParameterAccumulator, l_params](
      new AggregatingStateDescriptor[l_params, ParameterAccumulator, l_params](
        "global_model",
        new modelAccumulator,
        createTypeInformation[ParameterAccumulator]))

  }

  override def flatMap(in: workerMessage, collector: Collector[ControlMessage]): Unit = {
    in.request match {
      case 0 =>
        // A node requests the global hyperparameters
        if (started.value) sendMessage(in.workerId, collector) else requests.add(in.workerId)
      case 1 =>
        // This is the asynchronous push/pull
        updateGlobalState(in.parameters)
        if (in.workerId == 0 && !started.value) {
          pipeline_id.update(in.pipelineID)
          val request_iterator = requests.get.iterator
          while (request_iterator.hasNext) sendMessage(request_iterator.next, collector)
          requests.clear()
          started.update(true)
        }
        sendMessage(in.workerId, collector)
    }
  }

  override def updateGlobalState(localModel: l_params): Unit = {
    global_model add (localModel * (1.0 / (1.0 * getRuntimeContext.getExecutionConfig.getParallelism)))
  }

  override def sendMessage(siteID: Int, collector: Collector[ControlMessage]): Unit = {
    collector.collect(ControlMessage(UpdatePipelinePS, siteID, pipeline_id.value, Some(global_model.get), None))
  }

}

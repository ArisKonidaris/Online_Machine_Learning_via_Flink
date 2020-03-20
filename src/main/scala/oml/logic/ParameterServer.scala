package oml.logic

import oml.common.{Counter, LongAccumulator, ParameterAccumulator, modelAccumulator}
import oml.message.mtypes.{ControlMessage, workerMessage}
import oml.nodes.hub.CoordinatorLogic
import oml.mlAPI.parameters.LearningParameters
import org.apache.flink.api.common.state._
import org.apache.flink.configuration.Configuration
import org.apache.flink.streaming.api.scala.createTypeInformation
import org.apache.flink.util.Collector

class ParameterServer extends CoordinatorLogic[workerMessage, ControlMessage] {

  private var counter: AggregatingState[Long, Long] = _
  private var pipeline_id: ValueState[Int] = _
  private var started: ValueState[Boolean] = _
  private var requests: ListState[Int] = _
  private implicit var global_model: AggregatingState[LearningParameters, LearningParameters] = _

  override def open(parameters: Configuration): Unit = {

    pipeline_id = getRuntimeContext.getState(
      new ValueStateDescriptor[Int]("pipeline_id", createTypeInformation[Int], -1))

    started = getRuntimeContext.getState(
      new ValueStateDescriptor[Boolean]("started", createTypeInformation[Boolean], false))

    requests = getRuntimeContext.getListState(
      new ListStateDescriptor[Int]("requests", createTypeInformation[Int]))

    global_model = getRuntimeContext.getAggregatingState[LearningParameters, ParameterAccumulator, LearningParameters](
      new AggregatingStateDescriptor[LearningParameters, ParameterAccumulator, LearningParameters](
        "global_model",
        new modelAccumulator,
        createTypeInformation[ParameterAccumulator]))

    counter = getRuntimeContext.getAggregatingState[Long, Counter, Long](
      new AggregatingStateDescriptor[Long, Counter, Long](
        "counter",
        new LongAccumulator,
        createTypeInformation[Counter]))

  }

  override def flatMap(in: workerMessage, collector: Collector[ControlMessage]): Unit = {
    in.request match {
      case 0 =>
        // A node requests the global hyperparameters
        if (started.value && in.workerId != 0) sendMessage(in.workerId, collector) else requests.add(in.workerId)
      case 1 =>
        // This is the asynchronous push operation

        val params: LearningParameters = in.getParameters
          .asInstanceOf[Array[AnyRef]](0)
          .asInstanceOf[LearningParameters]

        updateGlobalState(params)
        counter.add(params.getFitted)
        if (in.workerId == 0 && !started.value) {
          pipeline_id.update(in.nodeID)
          val request_iterator = requests.get.iterator
          while (request_iterator.hasNext) sendMessage(request_iterator.next, collector)
          requests.clear()
          started.update(true)
        }
        sendMessage(in.workerId, collector)
    }
//    println("Pipeline " + request.getPipelineID + " has fitted " + counter.get() + " data points.")
  }

  override def updateGlobalState(localModel: LearningParameters): Unit = {
    global_model add (localModel * (1.0 / (1.0 * getRuntimeContext.getExecutionConfig.getParallelism)))
  }

  override def sendMessage(siteID: Int, collector: Collector[ControlMessage]): Unit = {
    collector.collect(ControlMessage(Some(1), siteID, pipeline_id.value, Some(global_model.get), None))
  }

}

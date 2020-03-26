package oml.logic

import oml.math.DenseVector
import oml.message.mtypes.{ControlMessage, workerMessage}
import oml.nodes.hub.CoordinatorLogic
import oml.parameters._
import org.apache.flink.api.common.state._
import org.apache.flink.configuration.Configuration
import org.apache.flink.streaming.api.scala.createTypeInformation
import org.apache.flink.util.Collector
import breeze.linalg.{DenseVector => BreezeDenseVector}
import oml.state.{AggregateDenseVectorFunction, Counter, DenseVectorAccumulator, LongAccumulator}

class ParameterServer extends CoordinatorLogic[workerMessage, ControlMessage] {

  private var counter: AggregatingState[Long, Long] = _
  private var pipelineId: ValueState[Int] = _
  private var started: ValueState[Boolean] = _
  private var requests: ListState[Int] = _
  private var modelDescription: ValueState[ParameterDescriptor] = _
  private implicit var globalAggregate: AggregatingState[BreezeDenseVector[Double], BreezeDenseVector[Double]] = _

  override def open(parameters: Configuration): Unit = {

    pipelineId = getRuntimeContext.getState(
      new ValueStateDescriptor[Int]("pipelineId", createTypeInformation[Int], -1))

    started = getRuntimeContext.getState(
      new ValueStateDescriptor[Boolean]("started", createTypeInformation[Boolean], false))

    requests = getRuntimeContext.getListState(
      new ListStateDescriptor[Int]("requests", createTypeInformation[Int]))

    globalAggregate = getRuntimeContext
      .getAggregatingState[BreezeDenseVector[Double], DenseVectorAccumulator, BreezeDenseVector[Double]](
        new AggregatingStateDescriptor(
          "globalAggregate",
          new AggregateDenseVectorFunction,
          createTypeInformation[DenseVectorAccumulator]))

    modelDescription = getRuntimeContext.getState(
      new ValueStateDescriptor[ParameterDescriptor](
        "modelDescription", createTypeInformation[ParameterDescriptor]))

    counter = getRuntimeContext.getAggregatingState[Long, Counter, Long](
      new AggregatingStateDescriptor[Long, Counter, Long](
        "counter",
        new LongAccumulator,
        createTypeInformation[Counter]))

  }

  override def flatMap(in: workerMessage, collector: Collector[ControlMessage]): Unit = {
    in.request match {
      case 0 =>
        // A node requests the global hyper parameters
        if (started.value && in.workerId != 0) sendMessage(in.workerId, collector) else requests.add(in.workerId)
      case 1 =>
        // This is the asynchronous push operation
        val params: ParameterDescriptor = in
          .getParameters
          .asInstanceOf[Array[AnyRef]](0)
          .asInstanceOf[ParameterDescriptor]

        updateGlobalState(params)
        if (in.workerId == 0 && !started.value) {
          modelDescription.update(
            new ParameterDescriptor(params.getParamSizes, null, params.getBucket, 0)
          )
          pipelineId.update(in.nodeID)
          val request_iterator = requests.get.iterator
          while (request_iterator.hasNext) sendMessage(request_iterator.next, collector)
          requests.clear()
          started.update(true)
        }
        sendMessage(in.workerId, collector)
    }
  }

  override def updateGlobalState(remoteModelDescriptor: ParameterDescriptor): Unit = {
    val remoteVector = BreezeDenseVector(remoteModelDescriptor.getParams.asInstanceOf[DenseVector].data)
    globalAggregate add (remoteVector * (1.0 / (1.0 * getRuntimeContext.getExecutionConfig.getParallelism)))
    counter.add(remoteModelDescriptor.getFitted)
  }

  override def sendMessage(siteID: Int, collector: Collector[ControlMessage]): Unit = {
    val model = modelDescription.value
    model.setParams(DenseVector.denseVectorConverter.convert(globalAggregate.get))
    model.setFitted(counter.get())
    collector.collect(ControlMessage(Some(1), siteID, pipelineId.value, Some(model), None))
  }

}

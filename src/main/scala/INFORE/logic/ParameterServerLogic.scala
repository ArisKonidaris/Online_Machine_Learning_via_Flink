package INFORE.logic

import INFORE.common.{Counter, IntegerAccumulator, ParameterAccumulator, modelAccumulator}
import INFORE.message.{LearningMessage, psMessage}
import INFORE.parameters.{LinearModelParameters, LearningParameters => lr_params}
import org.apache.flink.api.common.functions.RichFlatMapFunction
import org.apache.flink.api.common.state.{AggregatingState, AggregatingStateDescriptor, ValueState, ValueStateDescriptor}
import breeze.linalg.{DenseVector => BreezeDenseVector}
import org.apache.flink.api.java.utils.ParameterTool
import org.apache.flink.configuration.Configuration
import org.apache.flink.streaming.api.scala.createTypeInformation
import org.apache.flink.util.Collector

class ParameterServerLogic extends RichFlatMapFunction[(Int, Int, lr_params), LearningMessage] {

  private var workers: ValueState[Int] = _
  private var global_model: ValueState[lr_params] = _
  private implicit var ag: AggregatingState[lr_params, lr_params] = _
  private var updates: AggregatingState[Int, Int] = _

  override def flatMap(in: (Int, Int, lr_params), collector: Collector[LearningMessage]): Unit = {
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

    global_model = getRuntimeContext.getState(
      new ValueStateDescriptor[lr_params]("global_model",
        createTypeInformation[lr_params],
        LinearModelParameters(BreezeDenseVector.zeros[Double](1), 0.0)))

    updates = getRuntimeContext.getAggregatingState[Int, Counter, Int](
      new AggregatingStateDescriptor[Int, Counter, Int](
        "updates",
        new IntegerAccumulator,
        createTypeInformation[Counter]
      ))

    ag = getRuntimeContext.getAggregatingState[lr_params, ParameterAccumulator, lr_params](
      new AggregatingStateDescriptor[lr_params, ParameterAccumulator, lr_params](
        "a_global_model",
        new modelAccumulator,
        createTypeInformation[ParameterAccumulator]))
  }

  //  private def receiveMessage(in: (Int, Int, lr_params), collector: Collector[LearningMessage]): Unit = {
  //    try {
  //      global_model.update(updateGlobalModel(global_model, in._3, workers.value))
  //      sendMessage(in._2, collector)
  //    } catch {
  //      case _: Throwable =>
  //        global_model.update(in._3)
  //        for (i <- 0 until workers.value()) sendMessage(i, collector)
  //    }
  //  }
  //
  //  private def updateGlobalModel(globalModel: ValueState[lr_params], localModel: lr_params, k: Int): lr_params = {
  //    globalModel.value + (localModel * (1 / (1.0 * k)))
  //  }
  //
  //  private def sendMessage(id: Int, collector: Collector[LearningMessage]): Unit = {
  //    collector.collect(psMessage(id, global_model.value))
  //  }

  private def receiveMessage(in: (Int, Int, lr_params), collector: Collector[LearningMessage]): Unit = {
    updateGlobalModel(in._3, workers.value)
      sendMessage(in._2, collector)
    if (in._2 == 0 && updates.get == 0) for (i <- 1 until workers.value) sendMessage(i, collector)
  }

  private def updateGlobalModel(localModel: lr_params, k: Int)(implicit gModel: AggregatingState[lr_params, lr_params]): Unit = {
    gModel add (localModel * (1 / (1.0 * k)))
  }

  private def sendMessage(id: Int, collector: Collector[LearningMessage]): Unit = {
    collector.collect(psMessage(id, ag.get))
  }

}

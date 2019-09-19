package INFORE.logic

import INFORE.parameters.{LearningParameters => lr_params, LinearModelParameters}
import org.apache.flink.api.common.functions.RichFlatMapFunction
import org.apache.flink.api.common.state.{ValueState, ValueStateDescriptor}
import breeze.linalg.{DenseVector => BreezeDenseVector}
import org.apache.flink.configuration.Configuration
import org.apache.flink.streaming.api.scala.createTypeInformation
import org.apache.flink.util.Collector

class ParameterServerLogic(val k: Int) extends RichFlatMapFunction[(Int, Int, lr_params), String] {

  private var workers: ValueState[Int] = _
  private var global_model: ValueState[lr_params] = _

  override def flatMap(in: (Int, Int, lr_params), collector: Collector[String]): Unit = {
    receiveMessage(in, collector)
  }

  override def open(parameters: Configuration): Unit = {
    workers = getRuntimeContext.getState(
      new ValueStateDescriptor[Int]("workers", createTypeInformation[Int], k))
    global_model = getRuntimeContext.getState(
      new ValueStateDescriptor[lr_params]("global_model", createTypeInformation[lr_params],
        LinearModelParameters(BreezeDenseVector.zeros[Double](1), 0.0)))
  }

  private def receiveMessage(in: (Int, Int, lr_params), collector: Collector[String]): Unit = {
    try {
      global_model.update(updateGlobalModel(global_model, in._3, workers.value))
      sendMessage(in._2, collector)
    } catch {
      case _: Throwable =>
        global_model.update(in._3)
        for (i <- 0 until workers.value()) sendMessage(i, collector)
    }
  }

  private def updateGlobalModel(globalModel: ValueState[lr_params], localModel: lr_params, k: Int): lr_params = {
    globalModel.value + (localModel * (1 / (1.0 * k)))
  }

  private def sendMessage(id: Int, collector: Collector[String]): Unit = {
    collector.collect(id.toString + "," + global_model.value.toString)
  }

}

package OML.logic

import OML.common.{Counter, IntegerAccumulator, ParameterAccumulator, modelAccumulator}
import OML.message.{LearningMessage, psMessage}
import OML.parameters.{LearningParameters => l_params}
import org.apache.flink.api.common.functions.RichFlatMapFunction
import org.apache.flink.api.common.state.{AggregatingState, AggregatingStateDescriptor, ValueState, ValueStateDescriptor}
import org.apache.flink.api.java.utils.ParameterTool
import org.apache.flink.configuration.Configuration
import org.apache.flink.streaming.api.scala.createTypeInformation
import org.apache.flink.util.Collector

class ParameterServerLogic extends RichFlatMapFunction[(Int, Int, l_params), LearningMessage] {

  private var workers: ValueState[Int] = _
  private implicit var global_model: AggregatingState[l_params, l_params] = _
  private var updates: AggregatingState[Int, Int] = _

  override def flatMap(in: (Int, Int, l_params), collector: Collector[LearningMessage]): Unit = {
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
        "a_global_model",
        new modelAccumulator,
        createTypeInformation[ParameterAccumulator]))
  }

  private def receiveMessage(in: (Int, Int, l_params), collector: Collector[LearningMessage]): Unit = {
    updateGlobalModel(in._3, workers.value)
      sendMessage(in._2, collector)
    if (in._2 == 0 && updates.get == 0) for (i <- 1 until workers.value) sendMessage(i, collector)
  }

  private def updateGlobalModel(localModel: l_params, k: Int)
                               (implicit gModel: AggregatingState[l_params, l_params]): Unit = {
    gModel add (localModel * (1 / (1.0 * k)))
  }

  private def sendMessage(id: Int, collector: Collector[LearningMessage]): Unit = {
    collector.collect(psMessage(id, global_model.get))
  }

}

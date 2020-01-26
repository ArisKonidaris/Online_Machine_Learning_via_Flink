package OML.interact

import OML.message.ControlMessage
import OML.message.packages.{Container, PipelineContainer}
import org.apache.flink.api.common.functions.RichFlatMapFunction
import org.apache.flink.api.common.state.{MapState, MapStateDescriptor}
import org.apache.flink.util.Collector
import org.apache.flink.configuration.Configuration
import org.apache.flink.streaming.api.scala.createTypeInformation

class PipelineMap() extends RichFlatMapFunction[ControlMessage, ControlMessage] {

  private var pipeline_map: MapState[Int, Container] = _

  override def flatMap(in: ControlMessage, collector: Collector[ControlMessage]): Unit = {
    if (!pipeline_map.contains(in.pipelineID)) {
      pipeline_map.put(in.pipelineID, in.container.get)
      for (i <- 0 until getRuntimeContext.getExecutionConfig.getParallelism) {
        in.setWorkerID(i)
        collector.collect(in)
      }
      println(s"Pipeline ${in.pipelineID} loaded.")
    }
  }

  override def open(parameters: Configuration): Unit = {
    pipeline_map = getRuntimeContext.getMapState(
      new MapStateDescriptor[Int, Container]("pipeline_map",
        createTypeInformation[Int],
        createTypeInformation[Container]))
  }

}

package oml.utils.parsers.requestStream

import oml.message.mtypes.ControlMessage
import oml.message.packages.Container
import org.apache.flink.api.common.functions.RichFlatMapFunction
import org.apache.flink.api.common.state.{MapState, MapStateDescriptor}
import org.apache.flink.configuration.Configuration
import org.apache.flink.streaming.api.scala.createTypeInformation
import org.apache.flink.util.Collector

class PipelineMap() extends RichFlatMapFunction[ControlMessage, ControlMessage] {

  private var node_map: MapState[Int, Container] = _

  override def flatMap(in: ControlMessage, collector: Collector[ControlMessage]): Unit = {

    implicit val out: Collector[ControlMessage] = collector

    if (in.request == 0 && !node_map.contains(in.nodeID)) { // Create
      node_map.put(in.nodeID, in.container.get)
      sendControlMessage(in)
      println(s"Pipeline ${in.nodeID} created.")
    } else if ((in.request == -1 || in.request == -2) && node_map.contains(in.nodeID)) { // Inform or Update
      sendControlMessage(in)
    } else if (in.request == -3 && node_map.contains(in.nodeID)) { // Delete
      node_map.remove(in.nodeID)
      sendControlMessage(in)
      println(s"Pipeline ${in.nodeID} deleted.")
    }

  }

  private def sendControlMessage(in: ControlMessage)(implicit collector: Collector[ControlMessage]): Unit = {
    for (i <- 0 until getRuntimeContext.getExecutionConfig.getParallelism) {
      in.setWorkerID(i)
      collector.collect(in)
    }
  }

  override def open(parameters: Configuration): Unit = {
    node_map = getRuntimeContext.getMapState(
      new MapStateDescriptor[Int, Container]("node_map",
        createTypeInformation[Int],
        createTypeInformation[Container]))
  }

}

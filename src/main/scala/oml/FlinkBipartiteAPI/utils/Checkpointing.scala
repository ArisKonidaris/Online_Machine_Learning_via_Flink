package oml.FlinkBipartiteAPI.utils

import org.apache.flink.api.java.utils.ParameterTool
import org.apache.flink.runtime.state.filesystem.FsStateBackend
import org.apache.flink.streaming.api.TimeCharacteristic
import org.apache.flink.streaming.api.scala.StreamExecutionEnvironment


object Checkpointing {

  def enableCheckpointing()
                         (implicit env: StreamExecutionEnvironment,
                          params: ParameterTool): Unit = {
    val defaultStateBackend: String =
      if (params.get("local", "true").toBoolean)
        "file:///home/aris/IdeaProjects/oml1.2/checkpoints"
      else
        "hdfs://clu01.softnet.tuc.gr:8020/user/vkonidaris/checkpoints"

    env.setStreamTimeCharacteristic(TimeCharacteristic.EventTime)
    env.enableCheckpointing(params.get("checkInterval", "5000").toInt)
    env.setStateBackend(new FsStateBackend(params.get("stateBackend", defaultStateBackend)))
  }

}

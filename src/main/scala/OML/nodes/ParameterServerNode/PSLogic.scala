package OML.nodes.ParameterServerNode

import org.apache.flink.api.common.functions.FlatMapFunction
import org.apache.flink.streaming.api.checkpoint.CheckpointedFunction

abstract class PSLogic[T, U] extends FlatMapFunction[T, U]
  with CheckpointedFunction
  with ParameterServer[T, U]
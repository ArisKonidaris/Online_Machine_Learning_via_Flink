package INFORE.nodes.ParameterServerNode

import org.apache.flink.api.common.functions.RichFlatMapFunction

abstract class ParameterServerLogic[T, U] extends RichFlatMapFunction[T, U] with ParameterServer[T, U]

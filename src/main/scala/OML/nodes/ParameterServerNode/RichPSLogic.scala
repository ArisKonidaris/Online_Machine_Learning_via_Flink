package OML.nodes.ParameterServerNode

import org.apache.flink.api.common.functions.RichFlatMapFunction

abstract class RichPSLogic[T, U] extends RichFlatMapFunction[T, U] with ParameterServer[T, U]

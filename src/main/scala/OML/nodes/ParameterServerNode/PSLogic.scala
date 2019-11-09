package OML.nodes.ParameterServerNode

import org.apache.flink.api.common.functions.FlatMapFunction
import org.apache.flink.streaming.api.checkpoint.CheckpointedFunction
import org.apache.flink.util.Collector

/** Basic abstract logic of a parameter server implemented via a Flink FlatMapFunction
  *
  * @tparam InMsg  The input message type accepted by the parameter server
  * @tparam OutMsg The output message type the parameter server emits
  */
abstract class PSLogic[InMsg, OutMsg]
  extends FlatMapFunction[InMsg, OutMsg]
    with CheckpointedFunction
    with ParameterServer {
  def receiveMessage(in: InMsg, collector: Collector[OutMsg]): Unit

  def sendMessage(id: Int, collector: Collector[OutMsg]): Unit
}
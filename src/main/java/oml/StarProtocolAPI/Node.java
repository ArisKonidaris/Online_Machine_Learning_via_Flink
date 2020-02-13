package oml.StarProtocolAPI;


import oml.message.workerMessage;
import org.apache.flink.util.Collector;

import java.io.Serializable;

public interface Node extends Serializable {

    void receiveMsg(Integer operation, Serializable message);

    void receiveTuple(Serializable tuple);

    void merge(Node node);

    void send(Collector<workerMessage> out);

}

package oml.StarTopologyAPI.sites;

import oml.StarTopologyAPI.GenericWrapper;
import oml.StarTopologyAPI.network.Message;
import oml.StarTopologyAPI.network.Network;
import oml.StarTopologyAPI.operations.RemoteCallIdentifier;
import oml.mlAPI.dataBuffers.DataSet;

import java.io.Serializable;

public class BufferingWrap<D extends Serializable> extends GenericWrapper {

    protected DataSet<D> dataBuffer = new DataSet<>();
    protected DataSet<Message> messageBuffer = new DataSet<>();

    public BufferingWrap() {
        super();
    }

    public BufferingWrap(NodeId nodeId, Object node, Network network, NetworkDescriptor graph) {
        super(nodeId, node, network, graph);
    }

    @Override
    public void receiveMsg(NodeId source, RemoteCallIdentifier rpc, Serializable tuple) {

        // TODO: Do not forget to remove this after the implementation of the prediction job.
//        if (source == null) {
//            System.out.println(nodeId.getNodeId() +
//                    network.describe().networkId +
//                    ((MLWorker<?>) node).getPerformance((ListBuffer<Point>) tuple) +
//                    training_set.length() +
//                    ((ListBuffer<?>) tuple).length());
//            return;
//        }

        if (processMessages) {
            super.receiveMsg(source, rpc, tuple);
            if (processData) {
                assert(dataBuffer.isEmpty());
            } else {
                if (syncFutures == 0) {
                    processData = true;
                    processFromBuffer();
                }
            }
        } else messageBuffer.append(new Message(source, rpc, tuple));
    }

    @Override
    public void receiveTuple(Serializable tuple) {
        if (!processData) {
            dataBuffer.append((D) tuple);
        } else if (syncFutures > 0) {
            processData = false;
            dataBuffer.append((D) tuple);
        } else {
            if (dataBuffer.isEmpty()) {
                super.receiveTuple(tuple);
            } else {
                dataBuffer.append((D) tuple);
                processFromBuffer();
            }
        }
    }

    private void processFromBuffer() {
        while (syncFutures == 0 && dataBuffer.nonEmpty())
            super.receiveTuple(dataBuffer.remove(0));
        if (syncFutures != 0)
            processData = false;
    }

//    @Override
//    public void merge(Node[] nodes) {
//        for (node: Node <- nodes) {
//            require(node.isInstanceOf[BufferingWrapper])
//            setTrainingSet(training_set.merge(node.asInstanceOf[BufferingWrapper].getTrainingSet))
//        }
//        training_set.completeMerge()
//        super.merge(nodes)
//        broadcastProxy.asInstanceOf[PullPush].pullModel.to(node.updateModel)
//        processData = false
//    }

    public DataSet<D> getDataBuffer() {
        return dataBuffer;
    }

    public void setDataBuffer(DataSet<D> dataBuffer) {
        this.dataBuffer = dataBuffer;
    }

    public DataSet<Message> getMessageBuffer() {
        return messageBuffer;
    }

    public void setMessageBuffer(DataSet<Message> messageBuffer) {
        this.messageBuffer = messageBuffer;
    }
}

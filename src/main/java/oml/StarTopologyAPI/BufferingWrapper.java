package oml.StarTopologyAPI;

import oml.StarTopologyAPI.network.Mergeable;
import oml.StarTopologyAPI.network.Network;
import oml.StarTopologyAPI.network.Node;
import oml.StarTopologyAPI.operations.RemoteCallIdentifier;
import oml.StarTopologyAPI.sites.NodeId;
import oml.mlAPI.math.Point;
import oml.mlAPI.mlworkers.worker.MLPeriodicWorker;
import scala.collection.mutable.ListBuffer;


import java.io.Serializable;
import java.util.ArrayList;

public class BufferingWrapper<D extends Serializable> extends GenericWrapper {

    private DataBuffer<D> dataBuffer;

    public BufferingWrapper(NodeId nodeId, NodeInstance node, Network network, DataBuffer<D> dataBuffer) {
        super(nodeId, node, network);
        this.dataBuffer = dataBuffer;
    }

    @Override
    public void receiveMsg(NodeId source, RemoteCallIdentifier rpc, Serializable tuple) {

        // TODO: Do not forget to remove this after the implementation of the prediction job.
        if (source == null) {
            System.out.println(nodeId.getNodeId() + " , " +
                    network.describe().getNetworkId() + " , " +
                    ((MLPeriodicWorker) node).getPerformance((ListBuffer<Point>) tuple) + " , " +
                    dataBuffer.length() + " , " +
                    ((ListBuffer<Point>) tuple).length());
            return;
        }

        super.receiveMsg(source, rpc, tuple);
        if (!isBlocked()) {
            assert (dataBuffer.isEmpty());
        } else {
            if (syncFutures == 0) {
                unblock();
                processFromDataBuffer();
            }
        }
    }

    @Override
    public void receiveTuple(Serializable tuple) {
        if (isBlocked()) {
            dataBuffer.append((D) tuple);
        } else if (syncFutures > 0) {
            block();
            dataBuffer.append((D) tuple);
        } else {
            if (dataBuffer.isEmpty()) {
                super.receiveTuple(tuple);
            } else {
                dataBuffer.append((D) tuple);
                processFromDataBuffer();
            }
        }
    }

    private void processFromDataBuffer() {
        while (dataBuffer.nonEmpty() && syncFutures == 0)
            super.receiveTuple(dataBuffer.remove(0));
        if (syncFutures > 0)
            block();
    }

    @Override
    public void merge(Mergeable[] nodes) {
        super.merge(nodes);
        ArrayList<DataBuffer<D>> listOfBuffers = new ArrayList<>();
        for (Node node : (Node[]) nodes) {
            assert (node instanceof BufferingWrapper);
            listOfBuffers.add(((BufferingWrapper) node).getDataBuffer());
        }
        dataBuffer.merge(listOfBuffers.toArray(new DataBuffer[listOfBuffers.size()]));
    }

    public DataBuffer<D> getDataBuffer() {
        return dataBuffer;
    }

    public void setDataBuffer(DataBuffer<D> dataBuffer) {
        this.dataBuffer = dataBuffer;
    }

}

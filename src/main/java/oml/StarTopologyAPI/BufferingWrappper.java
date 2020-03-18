package oml.StarTopologyAPI;

import oml.StarTopologyAPI.network.Mergeable;
import oml.StarTopologyAPI.network.Network;
import oml.StarTopologyAPI.network.Node;
import oml.StarTopologyAPI.operations.RemoteCallIdentifier;
import oml.StarTopologyAPI.sites.NodeId;
import oml.math.Point;
import oml.mlAPI.mlworkers.worker.MLWorker;
import scala.collection.mutable.ListBuffer;

import java.io.Serializable;
import java.util.ArrayList;

public class BufferingWrappper<D extends Serializable> extends GenericWrapper {

    protected DataBuffer<D> dataBuffer;

    public BufferingWrappper() {
        super();
        dataBuffer = null;
    }

    public BufferingWrappper(NodeId nodeId, Object node, Network network, DataBuffer<D> dataBuffer) {
        super(nodeId, node, network);
        this.dataBuffer = dataBuffer;
    }

    @Override
    public void receiveMsg(NodeId source, RemoteCallIdentifier rpc, Serializable tuple) {

        // TODO: Do not forget to remove this after the implementation of the prediction job.
        if (source == null) {
            System.out.println(nodeId.getNodeId() +
                    network.describe().getNetworkId() +
                    ((MLWorker<?>) node).getPerformance((ListBuffer<Point>) tuple) +
                    dataBuffer.length() +
                    ((ListBuffer<?>) tuple).length());
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
        while (dataBuffer.nonEmpty())
            super.receiveTuple(dataBuffer.remove(0));
        if (syncFutures > 0)
            block();
    }

    @Override
    public void merge(Mergeable[] nodes) {
        super.merge(nodes);
        ArrayList<DataBuffer<D>> listOfBuffers = new ArrayList<>();
        for (Node node : (Node[]) nodes) {
            assert (node instanceof BufferingWrappper);
            listOfBuffers.add(((BufferingWrappper) node).getDataBuffer());
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

package oml.StarTopologyAPI.sites;

import java.io.Serializable;

/**
 * This is a basic Java class that implements a unique identifier
 * of a node in a Bipartite Network. A node's address is the by a {@link NodeType}
 * an Integer. The {@link NodeType} determines the type of the node (hub or spoke),
 * and the nodeId integer is the non-negative id of the node.
 *
 * nodeType: This is the type of node in the Bipartite Network.
 * nodeId: This should always be a non-negative value.
 *
 */
public class NodeId implements Serializable {

    protected NodeType nodeType; // This should always be a non negative value
    protected Integer nodeId; // The node id. This cannot be negative.

    public NodeId() {
        nodeType = null;
        nodeId = null;
    }

    public NodeId(NodeType nodeType, Integer nodeId) {
        checkNodeId(nodeId);
        this.nodeType = nodeType;
        this.nodeId = nodeId;
    }

    public NodeType getNodeType() {
        return nodeType;
    }

    public void setNodeType(NodeType nodeType) {
        this.nodeType = nodeType;
    }

    public Integer getNodeId() {
        return nodeId;
    }

    public void setNodeId(Integer nodeId) {
        checkNodeId(nodeId);
        this.nodeId = nodeId;
    }

    public String getID() {
        return nodeType + ":" + nodeId;
    }

    public void checkNodeId(Integer nodeId) {
        if (nodeId < 0)
            throw new RuntimeException("The id of a Node cannot be negative.");
    }

    // TODO: Rename to Spoke
    public boolean isSpoke() {
        return nodeType.equals(NodeType.SPOKE);
    }

    public boolean isHub() {
        return !isSpoke();
    }

}

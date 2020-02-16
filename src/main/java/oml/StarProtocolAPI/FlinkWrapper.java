package oml.StarProtocolAPI;

import com.sun.istack.NotNull;

import java.io.Serializable;

public class FlinkWrapper extends GenericWrapper {

    private Integer flink_worker_id;
    private Integer node_id;
    private WorkerGenerator generator;
    private Serializable data_buffer;

    public FlinkWrapper(@NotNull Integer flink_worker_id,
                        @NotNull Integer node_id,
                        WorkerGenerator generator) {
        super();
        this.flink_worker_id = flink_worker_id;
        this.node_id = node_id;
        this.generator = generator;
    }

    public FlinkWrapper(Serializable config,
                        @NotNull Integer flink_worker_id,
                        @NotNull Integer node_id,
                        WorkerGenerator generator) {
        super(generator.generate(config));
        this.flink_worker_id = flink_worker_id;
        this.node_id = node_id;
        this.generator = generator;
    }

    public void setNode(Serializable config) {
        setNode(generator.generate(config));
    }

    public void setNode(Serializable config, WorkerGenerator generator) {
        setNode(generator.generate(config));
        setGenerator(generator);
    }

    public WorkerGenerator getGenerator() {
        return generator;
    }

    public void setGenerator(WorkerGenerator generator) {
        this.generator = generator;
    }

    public int getFlink_worker_id() {
        return flink_worker_id;
    }

    public void setFlink_worker_id(int flink_worker_id) {
        this.flink_worker_id = flink_worker_id;
    }

    public int getNode_id() {
        return node_id;
    }

    public void setNode_id(int node_id) {
        this.node_id = node_id;
    }

    public Serializable getData_buffer() {
        return data_buffer;
    }

    public void setData_buffer(Serializable data_buffer) {
        this.data_buffer = data_buffer;
    }
}

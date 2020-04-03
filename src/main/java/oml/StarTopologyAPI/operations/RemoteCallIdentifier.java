package oml.StarTopologyAPI.operations;

import java.io.Serializable;

public class RemoteCallIdentifier implements Serializable {

    CallType call_type;
    String operation;
    long call_number;

    public RemoteCallIdentifier() {
    }

    public RemoteCallIdentifier(long call_number) {
        this(CallType.RESPONSE, null, call_number);
    }

    public RemoteCallIdentifier(CallType call_type, String operation, long call_number) {
        this.call_type = call_type;
        this.operation = operation;
        this.call_number = call_number;
    }

    public CallType getCallType() {
        return call_type;
    }

    public void setCallType(CallType call_type) {
        this.call_type = call_type;
    }

    public String getOperation() {
        return operation;
    }

    public void setOperation(String operation) {
        this.operation = operation;
    }

    public Long getCallNumber() {
        return call_number;
    }

    public void setCallNumber(Long call_number) {
        this.call_number = call_number;
    }
}

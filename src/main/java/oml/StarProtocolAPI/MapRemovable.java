package oml.StarProtocolAPI;

import java.io.Serializable;
import java.util.Map;

public class MapRemovable implements Serializable {
    private Map<?, ?> data_structure;

    public MapRemovable(Map<?, ?> data_structure) {
        this.data_structure = data_structure;
    }

    public void remove() {
        try {
            data_structure.remove(this);
        } catch (Exception e) {
            throw new RuntimeException(String.format("Could not remove node form Map %s", data_structure), e);
        }

    }
}

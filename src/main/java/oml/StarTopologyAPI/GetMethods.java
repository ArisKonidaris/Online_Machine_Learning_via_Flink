package oml.StarTopologyAPI;

import java.lang.reflect.Method;

public class GetMethods {
    public static Method[] getMethods(Object instance) {
        return instance.getClass().getDeclaredMethods();
    }
}

package com.mycompany.testsupport;

import java.lang.reflect.Field;

public final class SingletonReset {
    private SingletonReset() {
    }

    public static void resetSingleton(Class<?> clazz) {
        resetStaticField(clazz, "instance");
    }

    public static void resetStaticField(Class<?> clazz, String fieldName) {
        try {
            Field f = clazz.getDeclaredField(fieldName);
            f.setAccessible(true);
            f.set(null, null);
        } catch (Exception e) {
            throw new RuntimeException("Failed to reset " + clazz.getName() + "." + fieldName, e);
        }
    }
}

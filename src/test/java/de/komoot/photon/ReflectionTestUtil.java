package de.komoot.photon;

import java.lang.reflect.Field;

/**
 * Created by Sachin Dole on 2/12/2015.
 */
public class ReflectionTestUtil {

    public static <T> T getFieldValue(Object anObject, String fieldName) {
        return ReflectionTestUtil.getFieldValue(anObject, anObject.getClass(), fieldName);
    }

    public static <T> T getFieldValue(Object anObject, Class<?> clazz, String fieldName) {
        try {
            Field path = clazz.getDeclaredField(fieldName);
            path.setAccessible(true);
            return (T) path.get(anObject);
        } catch (IllegalAccessException e) {
            throw new RuntimeException("unable to get value of field", e);
        } catch (NoSuchFieldException e) {
            throw new RuntimeException("unable to get value of field", e);
        }
    }

    public static <T> void setFieldValue(Object anObject, Class<?> clazz, String fieldName, T value) {
        try {
            Field path = clazz.getDeclaredField(fieldName);
            path.setAccessible(true);
            path.set(anObject, value);
        } catch (IllegalAccessException e) {
            throw new RuntimeException("unable to get value of field (illegal)", e);
        } catch (NoSuchFieldException e) {
            throw new RuntimeException("unable to get value of field (missing)", e);
        }
    }

    public static <T> void setFieldValue(Object anObject, String fieldName, T value) {
        setFieldValue(anObject, anObject.getClass(), fieldName, value);
    }
}

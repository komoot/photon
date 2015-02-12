package de.komoot.photon;

import java.lang.reflect.Field;

/**
 * Created by Sachin Dole on 2/12/2015.
 */
public class ReflectionTestUtil {

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

}

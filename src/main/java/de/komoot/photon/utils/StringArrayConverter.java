package de.komoot.photon.utils;

import com.beust.jcommander.IStringConverter;

import java.util.Arrays;

/**
 * Converter function for JCommand that creates a String array from a comma separated list.
 */
public class StringArrayConverter implements IStringConverter<String[]> {
    @Override
    public String[] convert(String value) {
        if (value == null) {
            return new String[]{};
        }

        return Arrays.stream(value.split(","))
                .map(s -> s.trim())
                .filter(s -> !s.isEmpty())
                .toArray(String[]::new);
    }
}

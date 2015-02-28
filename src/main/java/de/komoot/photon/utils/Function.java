package de.komoot.photon.utils;

/**
 * Created by sachi_000 on 2/28/2015.
 */
public interface Function<I,O,E extends Exception> {
    O apply(I input) throws E;
}

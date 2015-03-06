package de.komoot.photon.utils;

/**
 * Created by sachi_000 on 2/20/2015.
 */
public interface OneWayConverter<A, B> {
    /**
     * Convert instance of type {@link A} to instance of type {@link B}
     *
     * @param anItem an instance of type {@link A}
     *
     * @return an instance of type {@link B{}
     */
    B convert(A anItem);
}

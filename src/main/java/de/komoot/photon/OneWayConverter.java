package de.komoot.photon;

/**
 * Created by sachi_000 on 2/20/2015.
 */
public interface OneWayConverter<A,B> {
    public B convert(A anItem);
}

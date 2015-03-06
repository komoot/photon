package de.komoot.photon;

/**
 * Created by sachi_000 on 2/20/2015.
 */
public interface Command<R, O> {
    R execute(O... operand);
}

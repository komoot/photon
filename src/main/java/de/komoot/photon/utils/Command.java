package de.komoot.photon.utils;

/**
 * Command interface
 * <p/>
 * Created by sachi_000 on 2/20/2015.
 */
public interface Command<R, O> {
    /**
     * Do something with operands of type {@link O} and return the result of type {@link R}
     * @param operand
     * @return
     */
    R execute(O... operand);
}

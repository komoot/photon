package de.komoot.photon;

/**
 * Exception caused by some error from the user side.
 * <p>
 * This exception type will be caught and the error printed without giving
 * the user a backtrace.
 */
public class UsageException extends RuntimeException {

    public UsageException(String message) {
        super(message);
    }
}

package de.komoot.photon.query;

/**
 * Exception thrown when parsing the input request failed.
 */
public class BadRequestException extends RuntimeException {
    private final int httpStatus;

    public BadRequestException(int httpStatusCode, String message) {
        super(message);
        this.httpStatus = httpStatusCode;
    }

    public int getHttpStatus() {
        return httpStatus;
    }
}

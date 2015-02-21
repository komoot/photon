package de.komoot.photon.query;

/**
 * Created by Sachin Dole on 2/12/2015.
 */
public class BadRequestException extends Exception{
    private final int httpStatus;

    public BadRequestException(int httpStatusCode, String message) {
        super(message);
        this.httpStatus = httpStatusCode;
    }

    public int getHttpStatus() {
        return httpStatus;
    }
}

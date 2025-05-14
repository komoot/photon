package de.komoot.photon.query;

import spark.Request;

public interface RequestFactory<T extends RequestBase> {

    T create(Request webRequest) throws BadRequestException;
}

package de.komoot.photon.query;

import io.javalin.http.Context;

public interface RequestFactory<T extends RequestBase> {

    T create(Context context);
}

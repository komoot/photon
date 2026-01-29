package de.komoot.photon.query;

import io.javalin.http.Context;
import org.jspecify.annotations.NullMarked;

@NullMarked
public interface RequestFactory<T extends RequestBase> {

    T create(Context context);
}

package de.komoot.photon.searcher;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.util.List;

/**
 * Convert a list of results into an output string.
 */
@NullMarked
public interface ResultFormatter {

    String convert(List<PhotonResult> results, String language,
                   boolean withGeometry, boolean withDebugInfo, @Nullable String queryDebugInfo) throws IOException;

    String formatError(String msg);
}

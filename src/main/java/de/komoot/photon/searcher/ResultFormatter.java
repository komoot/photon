package de.komoot.photon.searcher;

import java.io.IOException;
import java.util.List;

/**
 * Convert a list of results into an output string.
 */
public interface ResultFormatter {

    String convert(List<PhotonResult> results, String language,
                   boolean withGeometry, boolean withDebugInfo, String queryDebugInfo) throws IOException;

    String formatError(String msg);
}

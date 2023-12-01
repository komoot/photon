package de.komoot.photon.searcher;

import com.fasterxml.jackson.core.JsonProcessingException;

import java.util.List;

/**
 * Convert a list of results into an output string.
 */
public interface ResultFormatter {

    String convert(List<PhotonResult> results, String debugInfo) throws JsonProcessingException;
    String convert(PhotonResult result);
}

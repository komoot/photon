package de.komoot.photon.searcher;

/**
 * List of filter types.
 */
public enum TagFilterKind {
    /// Include an object if it matches the filter parameters.
    INCLUDE,
    // Do not include an object if it matches the given filter parameters.
    EXCLUDE,
    // Do not include an object when it matches the value parameter.
    EXCLUDE_VALUE
}

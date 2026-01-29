package de.komoot.photon.nominatim.model;

import org.jspecify.annotations.NullMarked;

import java.util.Date;

/**
 * Information about places in the database that need updating.
 */
@NullMarked
public record UpdateRow(Long placeId, boolean toDelete, Date updateDate) {

}

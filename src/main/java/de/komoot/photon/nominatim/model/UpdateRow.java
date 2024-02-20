package de.komoot.photon.nominatim.model;

import java.util.Date;

/**
 * Information about places in the database that need updating.
 */
public class UpdateRow {

    private Long placeId;
    private boolean toDelete;
    private Date updateDate;

    public UpdateRow(Long placeId, boolean toDelete, Date updateDate) {
        this.placeId = placeId;
        this.toDelete = toDelete;
        this.updateDate = updateDate;
    }

    public Long getPlaceId() {
        return placeId;
    }

    public boolean isToDelete() {
        return toDelete;
    }

    public Date getUpdateDate() {
        return updateDate;
    }
}

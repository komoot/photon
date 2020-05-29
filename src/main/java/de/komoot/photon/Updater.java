package de.komoot.photon;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * @author felix
 */
public interface Updater {
    public void create(PhotonDoc doc);

    public void update(PhotonDoc doc);

    public void delete(String id);
    
    /**
     * Delete matching documents that were generated from a specific OSM object 
     * 
     * @param osmType the type of OSM element
     * @param osmId the OSM id of the element
     * @param osmKey optional tag key
     * @param osmValue optional tag value
     */
    public void delete(@Nonnull String osmType, long osmId, @Nullable String osmKey, @Nullable String osmValue);


    public void finish();

    public void updateOrCreate(PhotonDoc updatedDoc);
}

package de.komoot.photon.elasticsearch;

import de.komoot.photon.PhotonDoc;

/**
 * Container class for an update action and a PhotonDoc
 * 
 * @author Simon
 *
 */
public class PhotonAction {

    public enum ACTION {CREATE, DELETE, UPDATE, UPDATE_OR_CREATE};
    
    public ACTION action;
    
    public long id;
    
    public PhotonDoc doc;
}

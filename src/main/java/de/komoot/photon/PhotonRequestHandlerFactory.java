package de.komoot.photon;

import de.komoot.photon.query.FilteredPhotonRequest;
import de.komoot.photon.query.PhotonRequest;

/**
 * Created by Sachin Dole on 2/20/2015.
 */
public class PhotonRequestHandlerFactory {
    
    public <R extends PhotonRequest> PhotonRequestHandler<R> createHandler(R request){
        if (request instanceof FilteredPhotonRequest){
            return (PhotonRequestHandler<R>) new FilteredPhotonRequestHandler();
        }else{
            return (PhotonRequestHandler<R>) new SimplePhotonRequestHandler();
        }
        
    }
}

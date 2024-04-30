package de.komoot.photon;

import org.apache.commons.lang3.NotImplementedException;

import java.io.FileNotFoundException;

public class JsonDumper implements Importer {

    public JsonDumper(String filename, String[] languages, String[] extraTags) throws FileNotFoundException {
        throw new NotImplementedException();
    }

    @Override
    public void add(PhotonDoc doc, int objectId) {
        throw new NotImplementedException();
    }

    @Override
    public void finish() {
        throw new NotImplementedException();
    }
}

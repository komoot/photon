package de.komoot.photon.nominatim.model;

import de.komoot.photon.PhotonDoc;
import org.jspecify.annotations.NullMarked;

import java.sql.ResultSet;
import java.sql.SQLException;

@NullMarked
public interface NominatimTableAccessor {

    PhotonDoc rowToDoc(ResultSet rs) throws SQLException;

    String makeBaseQuery(String countrySQLWhere);
}

package de.komoot.photon.searcher;

public enum GeometryType {
    UNKNOWN("unknown"),
    POLYGON("Polygon"),
    LINESTRING("LineString");

    private final String name;

    GeometryType(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}

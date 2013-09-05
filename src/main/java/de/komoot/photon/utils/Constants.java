package de.komoot.photon.utils;

public interface Constants {
    enum COLUMNS {
        CREATOR("creator"),
        USER_INPUT("user_input"),
        USER_LOCATION("user_location"),
        TYPE("type"),
        NAME("name"),
        COORDINATE("coordinate"),
        DISTANCE_TOLERANCE("distance_tolerance", "3000"),
        MAX_RESULT_INDEX("max_result_index", "3"), // we start counting with 0!
        STREET("street"),
        HOUSENUMBER("housenumber"),
        ZIP("zip"),
        CITY("city"),
        STATE("state"),
        COUNTRY("country"),
        LOCALE("locale", "de_DE");

        private final String defaultValue;
        /**
         * name of the column in CSV file
         */
        private final String id;

        COLUMNS(String id) {
            this(id, null);
        }

        COLUMNS(String id, String defaultValue) {
            this.defaultValue = defaultValue;
            this.id = id;
        }

        public String getDefaultValue() {
            return defaultValue;
        }

        public String getId() {
            return id;
        }
    }
}
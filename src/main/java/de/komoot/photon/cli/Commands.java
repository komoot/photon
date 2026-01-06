package de.komoot.photon.cli;

import java.util.Objects;

public enum Commands {
    CMD_IMPORT("import"),
    CMD_UPDATE("update"),
    CMD_UPDATE_INIT("update-init"),
    CMD_JSON_DUMP("dump-nominatim-db"),
    CMD_SERVE("serve");

    private final String cmd;

    Commands(String name) {
        this.cmd = name;
    }

    public String getCmd() {
        return cmd;
    }

    public static Commands byCommand(String label) {
    for (Commands e : values()) {
        if (Objects.equals(e.cmd, label)) {
            return e;
        }
    }
    return null;
}
}

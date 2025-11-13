package de.komoot.photon;

import io.javalin.http.Context;
import io.javalin.http.Handler;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

public class StatusRequestHandler implements Handler {
    private final Server server;
    private final String version;
    private final String gitCommit;

    protected StatusRequestHandler(Server server) {
        this.server = server;
        Attributes manifestAttributes = readManifestAttributes();
        this.version = getManifestAttribute(manifestAttributes, "Implementation-Version");
        this.gitCommit = getManifestAttribute(manifestAttributes, "Git-Commit");
    }

    @Override
    public void handle(@NotNull Context context) throws IOException {
        DatabaseProperties dbProperties = server.loadFromDatabase();
        String importDateStr = "";
        if (dbProperties.getImportDate() != null) {
            importDateStr = dbProperties.getImportDate().toInstant().toString();
        }
        Map<String, String> response = new LinkedHashMap<>();
        response.put("status", "Ok");
        response.put("import_date", importDateStr);
        response.put("version", version);
        response.put("git_commit", gitCommit);
        context.json(response);
    }

    private Attributes readManifestAttributes() {
        try {
            var resource = getClass().getResource("/META-INF/MANIFEST.MF");
            if (resource != null) {
                try (var stream = resource.openStream()) {
                    return new Manifest(stream).getMainAttributes();
                }
            }
        } catch (IOException e) {
            // Ignore, will return unknown values
        }
        return null;
    }

    private String getManifestAttribute(Attributes attributes, String name) {
        if (attributes != null) {
            String value = attributes.getValue(name);
            if (value != null) {
                return value;
            }
        }
        return "unknown";
    }
}

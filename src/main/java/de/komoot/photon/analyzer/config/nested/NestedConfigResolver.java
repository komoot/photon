package de.komoot.photon.analyzer.config.nested;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.nio.file.Path;

@NullMarked
class NestedConfigResolver {
    @Nullable private final Path baseDir;

    public NestedConfigResolver(@Nullable Path baseDir) {
        this.baseDir = baseDir == null ? null : baseDir.toAbsolutePath();
    }

    @Nullable public InputStream getStreamForFile(String fileDescription) throws FileNotFoundException {
        if (fileDescription.startsWith("/FILE/ ")) {
            if (baseDir != null && fileDescription.length() > 7) {
                var file = Path.of(fileDescription.substring(7));
                if (!file.isAbsolute()) {
                    file = baseDir.resolve(file);
                }
                return new FileInputStream(file.toFile());
            }
        } else if (fileDescription.startsWith("/BUILTIN/ ")) {
            if (fileDescription.length() > 10) {
                var stream = getClass().getResourceAsStream(fileDescription.substring(10));
                if (stream != null) {
                    return stream;
                }
            }
        } else {
            return null;
        }

        throw new FileNotFoundException(fileDescription);
    }
}

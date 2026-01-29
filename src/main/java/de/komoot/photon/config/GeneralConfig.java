package de.komoot.photon.config;

import com.beust.jcommander.Parameter;
import org.jspecify.annotations.NullMarked;

@NullMarked
public class GeneralConfig {
    public static final String GROUP="General options";

    @Parameter(names = "-j", category = GROUP, description = """
            Use given number of threads in parallel where possible
            """)
    private int threads = 1;

    @Parameter(names = "-h", category = GROUP, description = "Show help/usage")
    private boolean usage = false;

    public int getThreads() {
        return Integer.min(10, Integer.max(0, threads));
    }

    public boolean isUsage() {
        return this.usage;
    }
}

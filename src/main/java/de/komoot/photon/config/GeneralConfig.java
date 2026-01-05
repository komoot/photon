package de.komoot.photon.config;

import com.beust.jcommander.Parameter;

public class GeneralConfig {
    @Parameter(names = "-j", description = "Number of threads to use (only for selected operations).")
    private int threads = 1;

    @Parameter(names = "-h", description = "Show help / usage")
    private boolean usage = false;

    public int getThreads() {
        return Integer.min(10, Integer.max(0, threads));
    }

    public boolean isUsage() {
        return this.usage;
    }
}

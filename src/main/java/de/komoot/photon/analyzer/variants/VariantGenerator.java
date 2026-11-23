package de.komoot.photon.analyzer.variants;

import java.util.Set;

public interface VariantGenerator {

    Set<String> transform(Set<String> input);
}

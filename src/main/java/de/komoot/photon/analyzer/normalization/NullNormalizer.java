package de.komoot.photon.analyzer.normalization;

public class NullNormalizer implements Normalizer {

    private final static class SharedWrapper {
        private final static NullNormalizer SHARED = new NullNormalizer();

        public static NullNormalizer wrapped() { return SHARED; }
    }

    @Override
    public String transform(String input) {
        return input;
    }

    public static NullNormalizer shared() {
        return SharedWrapper.wrapped();
    }
}

package de.komoot.photon.es;

import static java.util.Collections.singletonMap;

import java.io.IOException;
import java.util.Map;

import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.env.Environment;
import org.elasticsearch.index.IndexSettings;
import org.elasticsearch.index.analysis.TokenFilterFactory;
import org.elasticsearch.indices.analysis.AnalysisModule;
import org.elasticsearch.indices.analysis.AnalysisModule.AnalysisProvider;
import org.elasticsearch.plugins.AnalysisPlugin;
import org.elasticsearch.plugins.Plugin;

import de.komoot.photon.es.analysis.HyphenationCompoundWordTokenFilterFactory;

public class PhotonPlugin extends Plugin implements AnalysisPlugin {

    @Override
    public Map<String, AnalysisProvider<TokenFilterFactory>> getTokenFilters() {
        return singletonMap("photon_hyphenation_decompounder",
                        requriesAnalysisSettings(HyphenationCompoundWordTokenFilterFactory::new));
    }
    
    private static <T> AnalysisModule.AnalysisProvider<T> requriesAnalysisSettings(AnalysisModule.AnalysisProvider<T> provider) {
        return new AnalysisModule.AnalysisProvider<T>() {
            @Override
            public T get(IndexSettings indexSettings, Environment environment, String name, Settings settings) throws IOException {
                return provider.get(indexSettings, environment, name, settings);
            }

            @Override
            public boolean requiresAnalysisSettings() {
                return true;
            }
        };
    }

}

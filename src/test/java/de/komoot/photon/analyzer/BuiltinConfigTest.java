package de.komoot.photon.analyzer;

import de.komoot.photon.PhotonDoc;
import de.komoot.photon.analyzer.config.AnalyzerConfig;
import de.komoot.photon.analyzer.variants.VariantProcessor;
import org.assertj.core.api.SoftAssertions;
import org.assertj.core.api.junit.jupiter.SoftAssertionsExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.Arrays;
import java.util.stream.Collectors;

@ExtendWith(SoftAssertionsExtension.class)
public class BuiltinConfigTest {
    private VariantProcessor.Transform make(String... langs) {
        var config = AnalyzerConfig.createDefault();
        var trans = config.createProcessor(Arrays.stream(langs).collect(Collectors.toSet()), config.getInputNormalizerRules());

        return trans.getTransformForDoc(new PhotonDoc());
    }

    @Test
    void testGerman(SoftAssertions soft) {
        var trans = make("de");

        soft.assertThat(trans.transform("de", "Hauptstraße"))
                .containsExactlyInAnyOrder("hauptstrasse", "hauptstr", "haupt strasse", "haupt str");
        soft.assertThat(trans.transform("de", "Heine-Strasse"))
                .containsExactlyInAnyOrder("heinestrasse", "heinestr", "heine strasse", "heine str");
        soft.assertThat(trans.transform("de", "Wartburg"))
                .containsExactlyInAnyOrder("wartburg", "wartbg");
        soft.assertThat(trans.transform("de", "Burg Meissen"))
                .containsExactlyInAnyOrder("burg meissen", "bg meissen");
        soft.assertThat(trans.transform("de", "Oberweissig"))
                .containsExactlyInAnyOrder("oberweissig", "ob weissig");
        soft.assertThat(trans.transform("de", "Ober Hof"))
                .containsExactlyInAnyOrder("ober hof", "ob  hof");

        soft.assertThat(trans.transform("de", "Österlicher Südfährenweg"))
                .containsExactlyInAnyOrder(
                        "österlicher südfährenweg",
                        "oesterlicher suedfaehrenweg",
                        "österlicher südfähren weg",
                        "oesterlicher suedfaehren weg",
                        "österlicher südfährenw",
                        "oesterlicher suedfaehrenw",
                        "österlicher südfähren w",
                        "oesterlicher suedfaehren w");
    }

    @Test
    void testFrench(SoftAssertions soft) {
        var trans = make("fr");

        soft.assertThat(trans.transform("fr", "L'eglise"))
                .containsExactlyInAnyOrder("lʼeglise");
    }

    @Test
    void testNorwegian(SoftAssertions soft) {
        var trans = make("no");

        soft.assertThat(trans.transform("no", "Stensgata"))
                .containsExactlyInAnyOrder("stensgata", "stensgaten", "stensgt");
        soft.assertThat(trans.transform("no", "Stensgaten"))
                .containsExactlyInAnyOrder("stensgata", "stensgaten", "stensgt");
        soft.assertThat(trans.transform("no", "Alfons gate"))
                .containsExactlyInAnyOrder("alfons gate", "alfons gt");
    }
}

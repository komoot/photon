package de.komoot.photon.nominatim.testdb;

import org.assertj.core.api.ListAssert;
import org.assertj.core.api.ObjectAssert;
import de.komoot.photon.Importer;
import de.komoot.photon.PhotonDoc;

import java.util.AbstractList;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

public class CollectingImporter extends AbstractList<PhotonDoc> implements Importer {
    private final List<PhotonDoc> docs = new ArrayList<>();
    private int finishCalled = 0;


    @Override
    public void add(Iterable<PhotonDoc> inputDocs)
    {
        for (var doc : inputDocs) {
            docs.add(doc);
        }
    }

    @Override
    public void finish() {
        ++finishCalled;
    }

    @Override
    public int size() {
        return docs.size();
    }

    @Override
    public PhotonDoc get(int idx) {
        return docs.get(idx);
    }

    public int getFinishCalled() {
        return finishCalled;
    }

    public ObjectAssert<PhotonDoc> assertThatByPlaceId(long placeId) {
        return assertThat(docs.stream().filter(d -> d.getPlaceId() == placeId))
                .hasSize(1)
                .first();
    }

    public ObjectAssert<PhotonDoc> assertThatByRow(PlacexTestRow row) {
        return assertThatByPlaceId(row.getPlaceId())
                .satisfies(row::assertEquals);
    }

    public ListAssert<PhotonDoc> assertThatAllByRow(OsmlineTestRow row) {
        return assertThat(docs.stream().filter(d -> d.getPlaceId() == row.getPlaceId()))
                .isNotEmpty()
                .allSatisfy(row::assertEquals);
    }

    public ListAssert<PhotonDoc> assertThatAllByRow(PlacexTestRow row) {
        return assertThat(docs.stream().filter(d -> d.getPlaceId() == row.getPlaceId()))
                .isNotEmpty()
                .allSatisfy(row::assertEquals);
    }
}

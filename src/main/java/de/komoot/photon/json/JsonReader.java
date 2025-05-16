package de.komoot.photon.json;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.komoot.photon.ConfigExtraTags;
import de.komoot.photon.PhotonDoc;
import de.komoot.photon.UsageException;
import de.komoot.photon.nominatim.ImportThread;
import de.komoot.photon.nominatim.model.AddressRow;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;

public class JsonReader {
    private static final Logger LOGGER = LogManager.getLogger();

    private final JsonParser parser;
    private NominatimDumpHeader header = null;
    private Map<String, Map<String, String>> countryNames = new HashMap<>();
    private Map<Long, AddressRow> addressCache = new HashMap<>();

    private boolean useFullGeometries = false;
    private ConfigExtraTags extraTags = new ConfigExtraTags();
    private String[] countryFilter = null;

    public JsonReader(File inputFile) throws IOException {
        parser = configureObjectMapper().createParser(inputFile);
        parser.nextToken();
    }

    public JsonReader(InputStream inputStream) throws IOException {
        parser = configureObjectMapper().createParser(inputStream);
        parser.nextToken();
    }

    private ObjectMapper configureObjectMapper() {
        final var mapper = new ObjectMapper();
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        return mapper;
    }

    public void setUseFullGeometries(boolean enable) {
        useFullGeometries = enable;
    }

    public void setExtraTags(ConfigExtraTags extraTags) {
        this.extraTags = extraTags;
    }

    public void setCountryFilter(String[] countries) {
        if (countries == null || countries.length == 0) {
            this.countryFilter = null;
        } else {
            this.countryFilter = Arrays.stream(countries).map(String::toUpperCase).toArray(String[]::new);
            Arrays.sort(this.countryFilter);
        }
    }

    public void readHeader() throws IOException {
        final String docType = readStartDocument();

        if (!NominatimDumpHeader.DOCUMENT_TYPE.equals(docType)) {
            LOGGER.error("Expected header document, got '{}' at {}", docType, parser.currentLocation());
            throw new UsageException("Invalid dump file.");
        }

        header = parser.readValueAs(NominatimDumpHeader.class);

        readEndDocument();
    }

    public Date getImportDate() {
        if (header == null) {
            throw new RuntimeException("Import date is only available after reading the file header.");
        }

        return header.getDataTimestamp();
    }

    public void readFile(ImportThread importThread) throws IOException {
        String docType = readStartDocument();
        String currentCountry = "NONE";
        boolean isSortedByCountry = header == null? false : header.isSortedByCountry();
        boolean hasAddressLines = header == null ? true : header.hasAddressLines();
        while (docType != null) {
            if (NominatimPlaceDocument.DOCUMENT_TYPE.equals(docType)) {
                Iterable<PhotonDoc> docs;
                if (parser.isExpectedStartObjectToken()) {
                    docs = parsePlaceDocument().asMultiAddressDocs(countryFilter);
                } else if (parser.isExpectedStartArrayToken()) {
                    docs = new ArrayList<>();
                    while (parser.nextToken() != JsonToken.END_ARRAY) {
                        final var doc = parsePlaceDocument().asSimpleDoc();
                        if (doc.isUsefulForIndex()
                                && (countryFilter == null || Arrays.binarySearch(countryFilter, doc.getCountryCode()) >= 0)) {
                            ((List) docs).add(doc);
                        }
                    }
                } else {
                    LOGGER.error("Place document must contain object or an array of objects at {}", parser.currentLocation());
                    throw new UsageException("Invalid json file.");
                }

                var it = docs.iterator();
                if (it.hasNext()) {
                    while (it.hasNext()) {
                        var doc = it.next();
                        if (doc.getCountryCode() != null) {
                            var names = countryNames.get(doc.getCountryCode());
                            if (names != null) {
                                doc.setCountry(names);
                            }
                        }

                        if (!it.hasNext()) {
                            if (isSortedByCountry &&
                                    doc.getCountryCode() != null && !currentCountry.equals(doc.getCountryCode())) {
                                addressCache.clear();
                                currentCountry = doc.getCountryCode();
                            }
                            if (hasAddressLines
                                    && doc.getRankAddress() > 0 && doc.getRankAddress() < 28
                                    && !doc.getName().isEmpty()) {
                                addressCache.put(
                                        doc.getPlaceId(),
                                        new AddressRow(
                                                doc.getName(), doc.getTagKey(), doc.getTagValue(),
                                                doc.getRankAddress()));
                            }
                        }
                    }

                    importThread.addDocument(docs);
                }
            } else if (CountryInfo.DOCUMENT_TYPE.equals(docType)) {
                if (!parser.isExpectedStartArrayToken()) {
                    LOGGER.error("CountryInfo document must contain an array of objects at {}", parser.currentLocation());
                    throw new UsageException("Invalid json file.");
                }
                while (parser.nextToken() != JsonToken.END_ARRAY) {
                    var cinfo = parser.readValueAs(CountryInfo.class);
                    countryNames.put(cinfo.getCountryCode().toUpperCase(), cinfo.getName());
                }
            } else {
                LOGGER.warn("Unknown document type '{}'. Ignored.", docType);
                parser.skipChildren();
            }

            readEndDocument();
            docType = readStartDocument();
        }
    }

    private NominatimPlaceDocument parsePlaceDocument() throws IOException {
        final var doc = parser.readValueAs(NominatimPlaceDocument.class);


        if (!useFullGeometries) {
            doc.disableGeometries();
        }

        doc.filterExtraTags(extraTags);

        doc.completeAddressLines(addressCache);
        return doc;
    }

    private String readStartDocument() throws IOException {
        if (!parser.hasCurrentToken()) {
            return null;
        }

        if (!parser.isExpectedStartObjectToken()) {
            LOGGER.error("Invalid document start. Got token {} at {}", parser.currentToken(), parser.currentLocation());
            throw new UsageException("Invalid dump file.");
        }

        final String fieldName = parser.nextFieldName();
        if (!"type".equals(fieldName)) {
            LOGGER.error("Unexpected field '{}' instead of 'type' at {}", fieldName, parser.currentLocation());
            throw new UsageException("Invalid dump file.");
        }

        final String documentType = parser.nextTextValue();

        while (parser.nextToken() != JsonToken.END_OBJECT) {
            if ("content".equals(parser.currentName())) {
                parser.nextToken();
                return documentType;
            } else {
                // ignore any extra information
                parser.nextToken();
                parser.skipChildren().nextToken();
            }
        }

        LOGGER.error("Missing 'properties' field at {}", parser.currentLocation());
        throw new UsageException("Invalid dump file.");
    }

    private void readEndDocument() throws IOException {
        while (parser.nextToken() != JsonToken.END_OBJECT) {
            if (!parser.isExpectedStartObjectToken()) {
                LOGGER.error("Unexpected token while reading end of document at {}", parser.currentLocation());
                throw new UsageException("Invalid dump file.");
            }

            // ignore any extra information
            parser.skipChildren().nextToken();
        }
        parser.nextToken();
    }
}

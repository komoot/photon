package de.komoot.photon.json;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.komoot.photon.PhotonDoc;
import de.komoot.photon.UsageException;
import de.komoot.photon.nominatim.ImportThread;
import org.slf4j.Logger;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class JsonReader {
    private static final Logger LOGGER = org.slf4j.LoggerFactory.getLogger(JsonReader.class);

    private final JsonParser parser;
    private NominatimDumpHeader header = null;

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
        while (docType != null) {
            if (NominatimPlaceDocument.DOCUMENT_TYPE.equals(docType)) {
                if (parser.isExpectedStartObjectToken()) {
                    importThread.addDocument(parser.readValueAs(NominatimPlaceDocument.class).asMultiAddressDocs());
                } else if (parser.isExpectedStartArrayToken()) {
                    List<PhotonDoc> docs = new ArrayList<>();
                    while (parser.nextToken() != JsonToken.END_ARRAY) {
                        docs.add(parser.readValueAs(NominatimPlaceDocument.class).asSimpleDoc());
                    }
                    importThread.addDocument(docs);
                } else {
                    LOGGER.error("place document must contain object or an array of objects at {}", parser.currentLocation());
                    throw new UsageException("Invalid json file.");
                }
            } else {
                LOGGER.warn("Unknown document type '{}'. Ignored.", docType);
                parser.skipChildren();
            }

            readEndDocument();
            docType = readStartDocument();
        }

    }

    private String readStartDocument() throws IOException {
        if (!parser.isExpectedStartObjectToken()) {
            LOGGER.info("End of file at token {}, position {}", parser.currentToken(), parser.currentLocation());
            return null;
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

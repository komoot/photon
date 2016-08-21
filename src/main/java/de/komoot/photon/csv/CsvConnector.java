package de.komoot.photon.csv;

import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Coordinate;
import com.neovisionaries.i18n.CountryCode;
import com.opencsv.CSVReader;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Point;
import de.komoot.photon.Importer;
import de.komoot.photon.PhotonDoc;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Export csv data
 *
 * @author Christoph Mayrhofer
 */
@Slf4j
public class CsvConnector {   
    
        static <T> T firstNonNull(T first, T ifFirstNull) {
            return (first != null ? first : ifFirstNull);
        }
        
        private String lineData(String[] line, String prop){
            return (this.columnIndices.containsKey(prop)) ? line[this.columnIndices.get(prop)] : null;
        }
        
        private Map<String, Integer> columnIndices;
        private Importer importer;

	public void setImporter(Importer importer) {
		this.importer = importer;
	}

	private static final PhotonDoc FINAL_DOCUMENT = new PhotonDoc(0, null, 0, null, null, null, null, null, null, 0, 0, null, null, 0, 0);

	private class ImportThread implements Runnable {
		private final BlockingQueue<PhotonDoc> documents;

		public ImportThread(BlockingQueue<PhotonDoc> documents) {
			this.documents = documents;
		}

		@Override
		public void run() {
			while(true) {
				PhotonDoc doc;
				try {
					doc = documents.take();
					if(doc == FINAL_DOCUMENT)
						break;
					importer.add(doc);
				} catch(InterruptedException e) { /* safe to ignore? */ }
			}
			importer.finish();
		}
	}

	/**
	 * parses every row in the csv file, creates a corresponding document and calls the {@link #importer} for every document
         *
         * @param csvFile
         * @throws FileNotFoundException
         * @throws IOException
         * 
         * The csv File may include any of the following columns names:
         * 
         * lat (required)
         * lon (required)
         * name (required if housenumber is not defined)
         * housenumber (optional)
         * street (optional)
         * city (optional)
         * postcode (optional)
         * state (optional)
         * countrycode (optional)
         * importance (optional)
         * [osmType, tagKey, tagValue] (osm specific/ optional)
        */
	public void readData(String csvFile) throws IOException { {
		log.info("start importing documents from csv file ...");
		final AtomicLong counter = new AtomicLong();

		final int progressInterval = 50000;
		final long startMillis = System.currentTimeMillis();

		final BlockingQueue<PhotonDoc> documents = new LinkedBlockingDeque<PhotonDoc>(20);
		Thread importThread = new Thread(new ImportThread(documents));
		importThread.start();

                CSVReader reader = new CSVReader(new FileReader(csvFile));
                String [] nextLine;
                
                // read column headers
                this.columnIndices = new HashMap<String, Integer>();
                nextLine = reader.readNext();
                for(int i = 0; i < nextLine.length; i++){
                    this.columnIndices.put(nextLine[i], i);
                }
                
                // read row data
                while ((nextLine = reader.readNext()) != null) {
                                                          
                    // the following PhotonDoc properties are ignored for the csv import, because they are OSM specific
                    long placeId = 0;
                    long osmId = 0;
                    long parentPlaceId = 0;
                    long linkedPlaceId = 0;
                    int rankSearch = 0;
                    Map<String, String> extratags = null;

                    // The following properties are OSM specific, but can be used for generic data to improve searchability
                    String osmType = lineData(nextLine, "osmType"); // set default to W?
                    String tagKey = lineData(nextLine, "tagKey"); // set default to building?
                    String tagValue = lineData(nextLine, "tagValue"); // set default to yes?
                    
                    // All other properties can be used universally for csv imports
                    Envelope bbox = null;
                    Point centroid = null;
                    if(lineData(nextLine, "lon") != null && !lineData(nextLine, "lon").isEmpty()
                       && lineData(nextLine, "lat") != null && !lineData(nextLine, "lat").isEmpty()){
                        double lon = Double.parseDouble(lineData(nextLine, "lon"));
                        double lat = Double.parseDouble(lineData(nextLine, "lat"));
                        GeometryFactory gf = new GeometryFactory();
                        Coordinate coord = new Coordinate(lon, lat);
                        centroid = gf.createPoint(coord);
                    }
                  
                    CountryCode countryCode = null;
                    if(lineData(nextLine, "countrycode") != null && !lineData(nextLine, "countrycode").isEmpty()){
                        countryCode = CountryCode.getByCode(lineData(nextLine, "countrycode"));
                    }

                    double importance = Double.parseDouble(firstNonNull(lineData(nextLine, "importance"), "0"));
                    String housenumber = lineData(nextLine, "housenumber");
                    
                    Map<String, String> nameMap = new HashMap<String, String>();
                    String name = lineData(nextLine, "name");
                    if(name != null && !name.isEmpty()){
                        nameMap = new HashMap<String, String>();
                    }
                    nameMap.put("name", name);
                
                    PhotonDoc doc = new PhotonDoc(
                            placeId,
                            osmType,
                            osmId,
                            tagKey,
                            tagValue,
                            nameMap, 
                            housenumber, 
                            extratags, 
                            bbox, 
                            parentPlaceId,
                            importance, 
                            countryCode, 
                            centroid, 
                            linkedPlaceId, 
                            rankSearch
                    );

                    String postcode = lineData(nextLine, "postcode");
                    if(postcode != null && !postcode.isEmpty()){
                        doc.setPostcode(postcode);
                    }
                    
                    Map<String, String> streetMap = new HashMap<String, String>();
                    String street = lineData(nextLine, "street");
                    if(street != null && !street.isEmpty()){  
                        streetMap.put("name", street);     
                    }
                    doc.setStreet(streetMap);
                    
                    Map<String, String> cityMap = new HashMap<String, String>();
                    String city = lineData(nextLine, "city");
                    if(city != null && !city.isEmpty()){
                        cityMap.put("name", city);
                    }
                    doc.setCity(cityMap);
                    
                    Map<String, String> stateMap = new HashMap<String, String>();
                    String state = lineData(nextLine, "state");
                    if(state != null && !state.isEmpty()){
                        stateMap.put("name", state); 
                    }
                    doc.setState(stateMap);
                    
                    Map<String, String> countryMap = new HashMap<String, String>();
                    if(countryCode != null){
                        countryMap.put("name", countryCode.getName());
                    }   
                    doc.setCountry(countryMap);
                              
                    if(!doc.isUsefulForIndex()) continue; // do not import document

                    while(true) {
                            try {
                                    documents.put(doc);
                            } catch(InterruptedException e) {
                                    log.warn("Thread interrupted while placing document in queue.");
                                    continue;
                            }
                            break;
                    }
                    if(counter.incrementAndGet() % progressInterval == 0) {
                            final double documentsPerSecond = 1000d * counter.longValue() / (System.currentTimeMillis() - startMillis);
                            log.info(String.format("imported %s documents [%.1f/second]", MessageFormat.format("{0}", counter.longValue()), documentsPerSecond));
                    }
                }

		while(true) {
			try {
				documents.put(FINAL_DOCUMENT);
				importThread.join();
			} catch(InterruptedException e) {
				log.warn("Thread interrupted while placing document in queue.");
				continue;
			}
			break;
		}
		log.info(String.format("finished import of %s photon documents.", MessageFormat.format("{0}", counter.longValue())));
	}
    }
}
_NAME_
=============

_NAME_ is an open source geocoder built for [OpenStreetMap](http://www.osm.org) data. It is based on [Apache Solr](http://lucene.apache.org/solr/) - an efficient and highly scalable search platform.

_NAME_ was developed by [komoot](www.komoot.de) and provides multilingual and search-as-you-type geocoder, a real life demo can be seen on [www.komoot.de](http://www.komoot.de)


The project consistes of three parts:

1. Solr configuration
2. Converter that creates a solr xml file from a [Nominatim](http://wiki.openstreetmap.org/wiki/Nominatim) database (Java)
3. Testing environment for developing (Java)

## How to use
### Import data
_NAME_ needs a dataset (addresses, streets, cities, ...). Solr provides numerous input formats, including xml. Currently you cannot convert an OpenStreetMap planet file to use it with _NAME_. Instead you can use the _NominatorImporter_ included in this project to dump a nominatim database to a xml file. Refer to Nominatim's [Installation Guide](http://wiki.openstreetmap.org/wiki/Nominatim/Installation) how to setup and fill a postgis database. It takes up to 10 days and sufficient RAM to import the entire world, you might prefer taking a smaller region.

Once you have a nominatim database you can run the _NominatorImporter_ to create the solr index

```bash
mvn compile exec:java -Dexec.mainClass=de.komoot.search.importer.NominatimImporter -Dexec.args="...args..."
```

command line arguments

 - ```-h``` postgis database host, e.g. _localhost_
 - ```-d``` database name, usually _Nominatim_
 - ```-u``` username of database user
 - ```-P``` password of database user
 - ```-p``` database port, optional default value is 5432
 - ```-f``` path of xml output file, e.g. _/Users/christoph/berlin.sorl.xml.gz_

A complete example:

```bash
mvn compile exec:java -Dexec.mainClass=de.komoot.search.importer.NominatimImporter -Dexec.args="-h localhost -d nominatim_island -u christoph -P christoph -f /Users/christoph/iceland.solr.xml.gz" > /home/christoph/island_import.log
```

This will take some time (Europe ~ 12 hours). 

If you just want to check out _NAME_ you can use our example dump of Iceland too ([src/main/solrindex/iceland.solr.xml.gz](src/main/solrindex/iceland.solr.xml.gz)). 

### Setup Solr
You need to install Apache Solr (tested with version 4.4). Using Mac OS X and homebrew you can type:

```bash
brew install solr
```

Start solr and pass the configuration directory that ist part of the project [src/main/solrconfig/](src/main/solrconfig/)

```bash
solr /Users/christoph/komoot/solr4/src/main/solrconfig/
```

Check Solr's admin interface on [http://localhost:8983/solr/](http://localhost:8983/solr/)

You can already query for places but no search results will be found as we have not imported any data yet. To do so we need to type:

```bash
curl "http://localhost:8983/solr/collection1/update?commit=true" -F stream.file=/Users/christoph/iceland.solr.xml
curl "http://localhost:8983/solr/collection1/update" -F stream.body=' <optimize />'
```

Don't forget to unzip the xml file first.

Now solr is up and running and filled with data. You can start searching for places:

[http://localhost:8983/solr/collection1/select?q=reykjavik](http://localhost:8983/solr/collection1/select?q=reykjavik&wt=json&indent=true)

## How to contribute

Join us in improving _NAME_ and feel free to contact us!

For developping we used a test-based approch, the test environment and testcases are located in [src/main/java/de/komoot/search/tests/](src/main/java/de/komoot/search/tests/). Every test consists of a xml file (test data) and csv file (test definition).
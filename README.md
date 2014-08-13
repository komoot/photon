# photon

_Photon_ is an open source geocoder built for [OpenStreetMap](http://www.osm.org) data. It is based on [elasticsearch](http://elasticsearch.org/) - an efficient, powerful and highly scalable search platform.

_Photon_ was started by [komoot](http://www.komoot.de) and provides search-as-you-type and multilingual support. It's used in production with thousands of requests per minute at [www.komoot.de](http://www.komoot.de). Find our public API on our project page [photon.komoot.de](http://photon.komoot.de).

We are a very young project, feel free to test and participate! The previous version based on solr is accessible in the [deprecated solr branch](https://github.com/komoot/photon/tree/deprecated-solr-version).

### Features
- high performance
- highly scalability
- search-as-you-type
- multilingual search
- location bias
- typo tolerance
- OSM data import (built upon [Nominatim](https://github.com/twain47/Nominatim)) inclusive continuous updates


### Installation

photon requires java, at least version 6.

get photon
```bash
wget http://photon.komoot.de/data/photon-0.1.jar
```

download search index (31 gb, worldwide, languages: English, German, French and Italian)
 ```bash
wget http://photon.komoot.de/data/photon_data_world_5-07-2014.tar.bz2
tar xvjf photon_data_world_5-07-2014.tar.bz2
ln -s photon_data photon_data_world_5-07-2014.tar.bz2
 ```
 
start photon
```bash
java -jar photon-0.1.jar
```

Check the URL `http://localhost:2322/api?q=berlin` to see if photon is running without problems. You may want to use our [leaflet plugin](https://github.com/komoot/leaflet.photon) to see the results on a map.

discover more of photon's feature with its usage `java -jar photon-0.1.jar -h`. 



### Customized Search Data
If you need search data in other languages or restricted to a country you will need to create your search data by your own.
Once you have your [nominatim](https://github.com/twain47/Nominatim) database ready, you can import the data to photon:

```bash
java -jar photon-0.1.jar -nominatim-import -host localhost -port 5432 -database nominatim -user nominatim -password ...
```

The import of worldwide data set will take some hours/days, ssd disk are recommended to accelerate nominatim queries.

A nominatim setup is also a requirement to have continuous updates. To keep in sync with the latest OSM changes run:

```bash
export NOMINATIM_DIR=/home/nominatim/...
./continuously_update_from_nominatim.sh
```

### Search API
#### Start Photon
```bash
java -jar photon-0.1.jar
```

#### Search
```
http://localhost:2322/api?q=berlin
```

#### Search with Location Bias
```
http://localhost:2322/api?q=berlin&lon=10&lat=52
```

#### Adapt Number of Results
```
http://localhost:2322/api?limit=2
```

#### Adjust Language
```
http://localhost:2322/api?q=berlin&lang=it
```

#### Results as GeoJSON
```json
  {
    "type": "FeatureCollection",
    "features": [
      {
        "type": "Feature",
        "geometry": {
          "coordinates": [
            13.438596,
            52.519854
          ],
          "type": "Point"
        },
        "properties": {
          "city": "Berlin",
          "country": "Germany",
          "name": "Berlin"
        }
      },{
      "type": "Feature",
        "geometry": {
          "coordinates": [
            61.195088,
            54.005826
          ],
          "type": "Point"
        },
        "properties": {
          "country": "Russia",
          "name": "Berlin",
          "postcode": "457130"
        }
      }
    ]
  }
```

### Metrics

Photon's search configuration was developed with a specific test framework. It is written in Python and [hosted separately](https://github.com/yohanboniface/osm-geocoding-tester).

### Contact
Let us know what you think about photon! Create a github ticket or drop us a mail in https://lists.openstreetmap.org/listinfo/photon

### Licence
Photon software is open source and licensed under [Apache License, Version 2.0](http://opensource.org/licenses/Apache-2.0)

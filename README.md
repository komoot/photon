# photon

_Photon_ is an open source geocoder built for [OpenStreetMap](http://www.osm.org) data. It is based on [elasticsearch](http://elasticsearch.org/) - an efficient, powerful and highly scalable search platform.

_Photon_ was started by [komoot](http://www.komoot.de) and provides search-as-you-type and multilingual support. It's used in production with thousands of requests per minute at [www.komoot.de](http://www.komoot.de). Find our public API and demo on [photon.komoot.de](http://photon.komoot.de).

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
wget http://photon.komoot.de/data/photon-0.2.2.jar
```

download search index (31G gb compressed, 56.3 gb uncompressed, worldwide coverage, languages: English, German, French and Italian)
 ```bash
wget -O - http://photon.komoot.de/data/photon_data_021_150112.tar.bz2 |
bzip2 -cd | tar x
# you can significantly speed up extracting using pbzip2:
wget -O - http://photon.komoot.de/data/photon_data_021_150112.tar.bz2 |
pbzip2 -cd | tar x
 ```
 
start photon
```bash
java -jar photon-0.2.2.jar
```

Check the URL `http://localhost:2322/api?q=berlin` to see if photon is running without problems. You may want to use our [leaflet plugin](https://github.com/komoot/leaflet.photon) to see the results on a map.

discover more of photon's feature with its usage `java -jar photon-0.2.2.jar -h`.



### Customized Search Data
If you need search data in other languages or restricted to a country you will need to create your search data by your own.
Once you have your [nominatim](https://github.com/twain47/Nominatim) database ready, you can import the data to photon:

```bash
java -jar photon-0.2.2.jar -nominatim-import -host localhost -port 5432 -database nominatim -user nominatim -password ... -languages es,fr
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
java -jar photon-0.2.2.jar
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
http://localhost:2322/api?q=berlin&limit=2
```

#### Adjust Language
```
http://localhost:2322/api?q=berlin&lang=it
```

#### Filter results by [tags and values](http://taginfo.openstreetmap.org/projects/nominatim#tags) 
*Note: not all tags on [link in the title](http://taginfo.openstreetmap.org/projects/nominatim#tags) are supported. Please see [nominatim source](https://github.com/openstreetmap/osm2pgsql/blob/master/output-gazetteer.cpp#L81) for an accurate list.*
If one or many query parameters named ```osm_tag``` are present, photon will attempt to filter results by those tags. In general, here is the expected format (syntax) for the value of osm_tag request parameters.

1. Include places with tag: ```osm_tag=key:value```
2. Exclude places with tag: ```osm_tag=!key:value```
3. Include places with tag key: ```osm_tag=key```
4. Include places with tag value: ```osm_tag=:value```
5. Exclude places with tag key: ```osm_tag=!key```
6. Exclude places with tag value: ```osm_tag=:!value```

For example, to search for all places named ```berlin``` with tag of ```tourism=museum```, one should construct url as follows:
```
http://localhost:2322/api?q=berlin&osm_tag=toursim:museum
```

Or, just by they key

```
http://localhost:2322/api?q=berlin&osm_tag=tourism
```

#### Results as GeoJSON
```json
{
  "features": [
    {
      "properties": {
        "name": "Berlin",
        "state": "Berlin",
        "country": "Germany",
        "osm_key": "place",
        "osm_value": "city",
        "osm_type": "N",
        "osm_id": 240109189
      },
      "type": "Feature",
      "geometry": {
        "type": "Point",
        "coordinates": [
          13.3888599,
          52.5170365
        ]
      }
    },
    {
      "properties": {
        "name": "Berlin Olympic Stadium",
        "street": "Olympischer Platz",
        "housenumber": "3",
        "postcode": "14053",
        "state": "Berlin",
        "country": "Germany",
        "osm_key": "leisure",
        "osm_value": "stadium",
        "osm_type": "W",
        "osm_id": 38862723,
        "extent": [
          13.23727,
          52.5157151,
          13.241757,
          52.5135972
        ]
      },
      "type": "Feature",
      "geometry": {
        "type": "Point",
        "coordinates": [
          13.239514674078611,
          52.51467945
        ]
      }
    }]
  }
```

### Metrics

Photon's search configuration was developed with a specific test framework. It is written in Python and [hosted separately](https://github.com/yohanboniface/osm-geocoding-tester).

### Contact
Let us know what you think about photon! Create a github ticket or drop us an email in https://lists.openstreetmap.org/listinfo/photon

### Licence
Photon software is open source and licensed under [Apache License, Version 2.0](http://opensource.org/licenses/Apache-2.0)

# photon

[![Build Status](https://secure.travis-ci.org/komoot/photon.png?branch=master)](http://travis-ci.org/komoot/photon)

_Photon_ is an open source geocoder built for [OpenStreetMap](http://www.osm.org) data. It is based on [elasticsearch](http://elasticsearch.org/) - an efficient, powerful and highly scalable search platform.

_Photon_ was started by [komoot](http://www.komoot.de) and provides search-as-you-type and multilingual support. It's used in production with thousands of requests per minute at [www.komoot.de](http://www.komoot.de). Find our public API and demo on [photon.komoot.de](http://photon.komoot.de).

### Contribution

All code contributions and bug reports are welcome!

For questions please send an email to our mailing list https://lists.openstreetmap.org/listinfo/photon

Feel free to test and participate!

### Licence

Photon software is open source and licensed under [Apache License, Version 2.0](http://opensource.org/licenses/Apache-2.0)

### Features
- high performance
- highly scalability
- search-as-you-type
- multilingual search
- location bias
- typo tolerance
- filter by osm tag and value
- filter by bounding box
- reverse geocode a coordinate to an address
- OSM data import (built upon [Nominatim](https://github.com/twain47/Nominatim)) inclusive continuous updates


### Installation

photon requires java, at least version 8.

Download the search index (53G gb compressed, worldwide coverage, languages: English, German, French and Italian). The search index is updated weekly and thankfully provided by [GraphHopper](https://www.graphhopper.com/) with the support of [lonvia](https://github.com/lonvia).

Make sure you have bzip2 or pbzip2 installed and execute one of these two commands in your shell. This will download, uncompress and extract the huge database in one step:

 ```bash
wget -O - http://download1.graphhopper.com/public/photon-db-latest.tar.bz2 | bzip2 -cd | tar x
# you can significantly speed up extracting using pbzip2 (recommended):
wget -O - http://download1.graphhopper.com/public/photon-db-latest.tar.bz2 | pbzip2 -cd | tar x
 ```
 
Now get photon, at least 0.3, from [the releases](https://github.com/komoot/photon/releases) and start it:

```bash
java -jar photon-*.jar
```

Use the `-data-dir` option to point to the parent directory of `photon_data` if that directory is not in the default location `./photon_data`. Before you request photon ElasticSearch needs to load some data into memory so be patient for a few seconds.

To use an older version of ElasticSearch please download the data from [here](http://download1.graphhopper.com/public/photon-ES-17-db-171019.tar.bz2) (Nov 2017) via wget as described above and use version [0.2.7 of photon](http://photon.komoot.de/data/photon-0.2.7.jar) (Oct 2016).

Check the URL `http://localhost:2322/api?q=berlin` to see if photon is running without problems. You may want to use our [leaflet plugin](https://github.com/komoot/leaflet.photon) to see the results on a map.

To enable CORS (cross-site requests), use `-cors-any` to allow any origin or `-cors-origin` with a specific origin as the argument. By default, CORS support is disabled.

discover more of photon's feature with its usage `java -jar photon-*.jar -h`.


### Customized Search Data
If you need search data in other languages or restricted to a country you will need to create your search data by your own.
Once you have your [nominatim](https://github.com/twain47/Nominatim) database ready, you can import the data to photon.

If you haven't already set a password for your nominatim database user, do it now (change user name and password as you like, below):

```bash
su postgres
psql
ALTER USER nominatim WITH ENCRYPTED PASSWORD 'mysecretpassword';
```
Import the data to photon:
```bash
java -jar photon-*.jar -nominatim-import -host localhost -port 5432 -database nominatim -user nominatim -password mysecretpassword -languages es,fr
```

The import of worldwide data set will take some hours/days, ssd disk are recommended to accelerate nominatim queries.

#### Updating from OSM via Nominatim

In order to update nominatim from OSM and then photon from nominatim, you must start photon with the nominatim database credentials on the command line:

```bash
java -jar photon-*.jar -host localhost -port 5432 -database nominatim -user nominatim -password ...
```

A nominatim setup is also a requirement to have continuous updates. To keep nominatim in sync with the latest OSM changes and to update photon with nominatim afterwards run:

```bash
export NOMINATIM_DIR=/home/nominatim/...
./continuously_update_from_nominatim.sh
```

If you have updated nominatim with another method, photon can be updated by making a HTTP GET request to `/nominatim-update`, e.g. with this command:

```bash
curl http://localhost:2322/nominatim-update
```


### Search API

#### Search
```
http://localhost:2322/api?q=berlin
```

#### Search with Location Bias
```
http://localhost:2322/api?q=berlin&lon=10&lat=52
```

Increase this bias (range is 0.1 to 10, default is 1.6)

```
http://localhost:2322/api?q=berlin&lon=10&lat=52&location_bias_scale=2
```

#### Reverse geocode a coordinate
```
http://localhost:2322/reverse?lon=10&lat=52
```

#### Adapt Number of Results
```
http://localhost:2322/api?q=berlin&limit=2
```

#### Adjust Language
```
http://localhost:2322/api?q=berlin&lang=it
```

#### Filter results by bounding box
Expected format is minLon,minLat,maxLon,maxLat. 
```
http://localhost:2322/api?q=berlin&bbox=9.5,51.5,11.5,53.5
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
http://localhost:2322/api?q=berlin&osm_tag=tourism:museum
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

### Related Projects

 - Photon's search configuration was developed with a specific test framework. It is written in Python and [hosted separately](https://github.com/yohanboniface/osm-geocoding-tester).
 - [R packge](https://github.com/rCarto/photon) to access photon's public API with [R](https://en.wikipedia.org/wiki/R_%28programming_language%29)

# photon

[![Continuous Integration](https://github.com/komoot/photon/workflows/CI/badge.svg)](https://github.com/komoot/photon/actions)

_photon_ is an open source geocoder built for [OpenStreetMap](https://openstreetmap.org) data. It is based on [elasticsearch](http://elasticsearch.org/) - an efficient, powerful and highly scalable search platform.

_photon_ was started by [komoot](http://www.komoot.de) and provides search-as-you-type and multilingual support. Find our public API and demo on [photon.komoot.io](http://photon.komoot.io). Until October 2020 the API was available under photon.komoot.**de**. Please update your apps accordingly.

### Contribution

All code contributions and bug reports are welcome!

For questions please send an email to our [mailing list](https://lists.openstreetmap.org/listinfo/photon).

Feel free to test and participate!

### Licence

photon software is open source and licensed under [Apache License, Version 2.0](https://opensource.org/licenses/Apache-2.0)

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
- OSM data import (built upon [Nominatim](https://nominatim.org)) inclusive continuous updates

### Installation

photon requires Java, at least version 11.

Download the search index (72G GB compressed, 159GB uncompressed as of 2023-10-26, worldwide coverage, languages: English, German, French and local name). The search index is updated weekly and thankfully provided by [GraphHopper](https://www.graphhopper.com/) with the support of [lonvia](https://github.com/lonvia).
Now get the latest version of photon from [the releases](https://github.com/komoot/photon/releases).

Make sure you have bzip2 or pbzip2 installed and execute one of these two commands in your shell. This will download, uncompress and extract the huge database in one step:

```bash
wget -O - https://download1.graphhopper.com/public/photon-db-latest.tar.bz2 | bzip2 -cd | tar x
# you can significantly speed up extracting using pbzip2 (recommended):
wget -O - https://download1.graphhopper.com/public/photon-db-latest.tar.bz2 | pbzip2 -cd | tar x
```

### Building

photon uses [gradle](https://gradle.org) for building. To build the package
from source make sure you have a JDK installed. Then run:

```
./gradlew app:es_embedded:build
```

This will build and test photon. The final jar can be found in `target`.

#### Experimental OpenSearch version

The repository also contains a version that runs against the latest
version of [OpenSearch](https://opensearch.org/). This version is still
experimental. To build the OpenSearch version run:

```
./gradlew app:opensearch:build
```

The final jar can be found in `target/photon-opensearch-<VERSION>.jar`.

Indexes produced by this version are not compatible with the ElasticSearch
version. There are no prebuilt indexes available. You need to create your
own export from a Nominatim database. See 'Customized Search Data' below.

### Usage

Start photon with the following command:

```bash
java -jar photon-*.jar
```

Use the `-data-dir` option to point to the parent directory of `photon_data` if that directory is not in the default location `./photon_data`. Before you can send requests to photon, ElasticSearch needs to load some data into memory so be patient for a few seconds.

Check the URL `http://localhost:2322/api?q=berlin` to see if photon is running without problems. You may want to use our [leaflet plugin](https://github.com/komoot/leaflet.photon) to see the results on a map.

To enable CORS (cross-site requests), use `-cors-any` to allow any origin or `-cors-origin` with a specific origin as the argument. By default, CORS support is disabled.

Discover more of photon's features with its usage `java -jar photon-*.jar -h`. The available options are as follows:

```
-h                    Show help / usage

-cluster              Name of elasticsearch cluster to put the server into (default is 'photon')

-transport-addresses  The comma separated addresses of external elasticsearch nodes where the
                      client can connect to (default is an empty string which forces an internal node to start)

-nominatim-import     Import nominatim database into photon (this will delete previous index)

-nominatim-update     Fetch updates from nominatim database into photon and exit (this updates the index only
                      without offering an API)

-languages            Languages nominatim importer should import and use at run-time, comma separated (default is 'en,fr,de,it')

-default-language     Language to return results in when no explicit language is choosen by the user

-country-codes        Country codes filter that nominatim importer should import, comma separated. If empty full planet is done

-extra-tags           Comma-separated list of additional tags to save for each place

-synonym-file         File with synonym and classification terms

-json                 Import nominatim database and dump it to a json like files in (useful for developing)

-host                 Postgres host (default 127.0.0.1)

-port                 Postgres port (default 5432)

-database             Postgres host (default nominatim)

-user                 Postgres user (default nominatim)

-password             Postgres password (default '')

-data-dir             Data directory (default '.')

-listen-port          Listen to port (default 2322)

-listen-ip            Listen to address (default '0.0.0.0')

-cors-any             Enable cross-site resource sharing for any origin (default CORS not supported)

-cors-origin          Enable cross-site resource sharing for the specified origins, comma separated (default CORS not supported)

-enable-update-api    Enable the additional endpoint /nominatim-update, which allows to trigger updates
                      from a nominatim database
```

### Customized Search Data

If you need search data in other languages or restricted to a country you will need to create your search data by your own.
Once you have your [Nominatim](https://nominatim.org) database ready, you can import the data to photon.

If you haven't already set a password for your Nominatim database user, do it now (change user name and password as you like, below):

```bash
su postgres
psql
ALTER USER nominatim WITH ENCRYPTED PASSWORD 'mysecretpassword';
```

Import the data to photon:

```bash
java -jar photon-*.jar -nominatim-import -host localhost -port 5432 -database nominatim -user nominatim -password mysecretpassword -languages es,fr
```

The import of worldwide data set will take some hours/days, SSD/NVME disks are recommended to accelerate Nominatim queries.

#### Updating from OSM via Nominatim

To update an existing Photon database from Nominatim, first prepare the
Nominatim database with the appropriate triggers:

```bash
java -jar photon-*.jar -database nominatim -user nominatim -password ... -nominatim-update-init-for update_user
```

This script must be run with a user that has the right to create tables,
functions and triggers.

'update-user' is the PostgreSQL user that will be used when updating the
Photon database. The user needs read rights on the database. The necessary
update rights will be granted during initialisation.

Now you can run updates on Nominatim using the usual methods as described
in the [documentation](https://nominatim.org/release-docs/latest/admin/Update/).
To bring the Photon database up-to-date, stop the Nominatim updates and
then run the Photon update process:

```bash
java -jar photon-*.jar -database nominatim -user nominatim -password ... -nominatim-update
```

You can also run the photon process with the update API enabled:

```bash
java -jar photon-*.jar -enable-update-api -database nominatim -user nominatim -password ...
```

Then you can trigger updates like this:

```bash
curl http://localhost:2322/nominatim-update
```

This will only start the updates. To check if the updates have finished,
use the status API:

```bash
curl http://localhost:2322/nominatim-update/status
```

It returns a single JSON string `"BUSY"` when updates are in progress or
`"OK"` when another update round can be started.

For your convenience, this repository contains a script to continuously update
both Nominatim and Photon using Photon's update API. Make sure you have
Photon started with `-enable-update-api` and then run:

```bash
export NOMINATIM_DIR=/srv/nominatim/...
./continuously_update_from_nominatim.sh
```

where `NOMINATIM_DIR` is the project directory of your Nominatim installation.

### Search API

#### Search

```
http://localhost:2322/api?q=berlin
```

#### Search with Location Bias

```
http://localhost:2322/api?q=berlin&lon=10&lat=52
```

There are two optional parameters to influence the location bias. 'zoom'
describes the radius around the center to focus on. This is a number that
should correspond roughly to the map zoom parameter of a corresponding map.
The default is `zoom=16`.

The `location_bias_scale` describes how much the prominence of a result should
still be taken into account. Sensible values go from 0.0 (ignore prominence
almost completely) to 1.0 (prominence has approximately the same influence).
The default is 0.2.

```
http://localhost:2322/api?q=berlin&lon=10&lat=52&zoom=12&location_bias_scale=0.1
```

#### Reverse geocode a coordinate

```
http://localhost:2322/reverse?lon=10&lat=52
```

An optional radius parameter can be used to specify a value in kilometers
to reverse geocode within. The value has to be greater than 0 and lower than 5000.

```
http://localhost:2322/reverse?lon=10&lat=52&radius=10
```

#### Adapt Number of Results

```
http://localhost:2322/api?q=berlin&limit=2
```

#### Adjust Language

```
http://localhost:2322/api?q=berlin&lang=it
```

If omitted the ['accept-language' HTTP header](https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Accept-Language)
will be used (browsers set this by default). If neither is set the local name of the place is returned. In OpenStreetMap
data that's usually the value of the `name` tag, for example the local name for Tokyo is 東京都.

#### Filter results by bounding box

Expected format is minLon,minLat,maxLon,maxLat.

```
http://localhost:2322/api?q=berlin&bbox=9.5,51.5,11.5,53.5
```

#### Filter results by [tags and values](https://taginfo.openstreetmap.org/projects/nominatim#tags)

_Note: the filter only works on principal OSM tags and not all OSM tag/value combinations can be searched. The actual list depends on the import style used for the Nominatim database (e.g. [settings/import-full.style](https://github.com/osm-search/Nominatim/blob/master/settings/import-full.style)). All tag/value combinations with a property 'main' are included in the photon database._
If one or many query parameters named `osm_tag` are present, photon will attempt to filter results by those tags. In general, here is the expected format (syntax) for the value of osm_tag request parameters.

1. Include places with tag: `osm_tag=key:value`
2. Exclude places with tag: `osm_tag=!key:value`
3. Include places with tag key: `osm_tag=key`
4. Include places with tag value: `osm_tag=:value`
5. Exclude places with tag key: `osm_tag=!key`
6. Exclude places with tag value: `osm_tag=:!value`

For example, to search for all places named `berlin` with tag of `tourism=museum`, one should construct url as follows:

```
http://localhost:2322/api?q=berlin&osm_tag=tourism:museum
```

Or, just by they key

```
http://localhost:2322/api?q=berlin&osm_tag=tourism
```

You can also use this feature for reverse geocoding. Want to see the 5 pharmacies closest to a location ?

```
http://localhost:2322/reverse?lon=10&lat=52&osm_tag=amenity:pharmacy&limit=5
```

#### Filter results by layer

List of available layers:

- house
- street
- locality
- district
- city
- county
- state
- country
- other (e.g. natural features)

```
http://localhost:2322/api?q=berlin&layer=city&layer=locality
```

Example above will return both cities and localities.

#### Results as GeoJSON

```json
{
  "features": [
    {
      "properties": {
        "name": "Berlin",
        "state": "Berlin",
        "country": "Germany",
        "countrycode": "DE",
        "osm_key": "place",
        "osm_value": "city",
        "osm_type": "N",
        "osm_id": 240109189
      },
      "type": "Feature",
      "geometry": {
        "type": "Point",
        "coordinates": [13.3888599, 52.5170365]
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
        "countrycode": "DE",
        "osm_key": "leisure",
        "osm_value": "stadium",
        "osm_type": "W",
        "osm_id": 38862723,
        "extent": [13.23727, 52.5157151, 13.241757, 52.5135972]
      },
      "type": "Feature",
      "geometry": {
        "type": "Point",
        "coordinates": [13.239514674078611, 52.51467945]
      }
    }
  ]
}
```

### Structured queries

The OpenSeach based version of photon has opt-in support for structured queries. See [docs/structured.md](docs/structured.md) for details. Please note that structured queries are disabled for photon.komoot.io. 

### Related Projects

- photon's search configuration was developed with a specific test framework. It is written in Python and [hosted separately](https://github.com/yohanboniface/osm-geocoding-tester).
- [R package](https://github.com/rCarto/photon) to access photon's public API with [R](https://www.r-project.org/)
- [PHP Geocoder provider](https://github.com/geocoder-php/photon-provider) to access photon's public API with PHP using the [GeoCoder Package](https://github.com/geocoder-php/Geocoder)

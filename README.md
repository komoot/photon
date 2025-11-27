# About photon

[![Continuous Integration](https://github.com/komoot/photon/workflows/CI/badge.svg)](https://github.com/komoot/photon/actions)

_photon_ is an open source geocoder built for
[OpenStreetMap](https://openstreetmap.org) data. It is based on
[elasticsearch](http://elasticsearch.org/)/[OpenSearch](https://opensearch.org/) -
an efficient, powerful and highly scalable search platform.

_photon_ was started by [komoot](http://www.komoot.de) who also provide the
public demo server at [photon.komoot.io](https://photon.komoot.io).

## Features

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
- import and export dumps in concatenated JSON format

## Demo server

You can try out photon via the demo server at
[https://photon.komoot.io](http://photon.komoot.io). You are welcome to use
the API for your project as long as the number of requests stay in a reasonable
limit. Extensive usage will be throttled or completely banned. We do not
give guarantees for availability and reserve the right to implement changes
without notice.

*If you have a larger number of requests to make, please consider setting up
your own private instance. It is as simple as downloading two files and starting
the server. See instructions below.*

# Installation and Usage

## photon ElasticSearch vs. photon OpenSearch

photon was originally built on ElasticSearch. For technical reasons, we are
stuck with a very old an unsupported version 5.6 of ElasticSearch. Over the
last year, photon has been ported to the latest version of OpenSearch. This
version can now be considered stable. When setting up a new instance of
photon, please use **photon-opensearch**. This will become the default
and only available version soon.

## Requirements

photon requires Java, version 21+.

If you want to run against an external database instead of using the embedded
server, OpenSearch 3.x is needed.

A planet-wide database requires about 220GB disk space (as of 2025, grows by
about 10% a year). Using SSDs for storage is strongly recommended, NVME would
even be better. At least 64GB RAM are recommended for smooth operations, more,
if the server takes significant load. Running photon with less RAM is possible
but consider increasing the available heap (using `java -Xmx8G -jar ...` for
example). Be careful to make sure that there remains enough free RAM that the
system doesn't start swapping.

If you want to import data from Nominatim, there are additional
[requirements for Nominatim](https://nominatim.org/release-docs/latest/admin/Installation/#prerequisites).

## Using the release binaries and extracts

This is the easiest way to set up a self-hosted photon instance.
Pre-built jar files can be downloaded from the
[Github release page](https://github.com/komoot/photon/releases/latest).
Preferably get the photon-opensearch jar file.

[GraphHopper](https://www.graphhopper.com/) kindly provides weekly updated
dumps of the photon database at
[https://download1.graphhopper.com/public](https://download1.graphhopper.com/public).
The dumps are available for the world-wide dataset and for selected country datasets.
The dumps contain names in English, German, French and local language. There
is no support for
[full geometry output](https://github.com/komoot/photon/pull/823). If you need
this feature, you need to import your own database, see below.

For ElasticSearch photon use:

* world-wide: `https://download1.graphhopper.com/public/`
* country extracts: `https://download1.graphhopper.com/public/extracts/`

For OpenSearch photon use:

* world-wide: `https://download1.graphhopper.com/public/experimental/`
* country extracts: `https://download1.graphhopper.com/public/experimental/extracts/`

Make sure you have bzip2 or pbzip2 installed. Do not use WinRAR for unpacking,
it is known to have issues with the files. Execute one of these two
commands in your shell to download, uncompress and extract the huge
database in one step:

```
wget -O - https://download1.graphhopper.com/public/photon-db-planet-0.7OS-latest.tar.bz2 | bzip2 -cd | tar x
# you can significantly speed up extracting using pbzip2 (recommended):
wget -O - https://download1.graphhopper.com/public/photon-db-planet-0.7OS-latest.tar.bz2 | pbzip2 -cd | tar x
```

Don't forget to adapt the directory to **match your photon version**.

### Updating photon with a new version of the archive

You need to swap out the databases atomically: download and unpack the new version, swap the directories to put the new directory in place of the old one, then restart photon. Now delete the old database. This unfortunately means you need twice the space of the database for updates.

Do not unpack the database in place of the old one. This will lead to corrupted data.


## Building photon from scratch

photon uses gradle for building. To build the package from source make sure you have a JDK installed. Then run:

```
./gradlew build
```

This will build and test photon in the ElasticSearch and OpenSearch version.
The final jar can be found in the `target` directory.

## Usage


Start photon with the following command:

```
java -jar photon-*.jar
```

Use `-data-dir` to point to the _parent_ directory of the `photon_data` directory
with the database. By default photon will look for a `photon_data` directory
in the current working directory.

Alternatively, you can make photon connect to an external ElasticSearch/OpenSearch
instance. Use the parameter `-transport-addresses` to define a comma-separated
list of addresses of external nodes.

### Running photon

When photon is started without any parameters, it starts up a webserver with the
photon API at `http://localhost:2322`. You can
change the listening address with `-listen-ip <ip address>` and the port with
`-listen-port <port number>`. A description of the API endpoints is at the
end of this README.

To enable CORS (cross-site requests), use `-cors-any` to allow any origin or
`-cors-origin` with a specific origin as the argument. By default, CORS support
is disabled.

`-languages <languages>` restricts which languages are supported to be searched
and returned by the API. This may be used to temporarily restrict the language
to a different set than what was originally imported.

`-default-language` allows to chose in which language results are returned
when the user hasn't explicitly (or implicitly via the Accept-Language header)
chosen a language. The default is to return all results in the local language.

`-enable-update-api` adds a special endpoint that triggers updates from a
Nominatim API, see "Updating data via Nominatim" below. If this option is
enabled, you also need to add the connection parameters for the database.

`-max-results` and `-max-reverse-results` can be used to change the upper limit
of the `limit` parameter, which defines the maximum number of results returned,
for forward (`/api`) and reverse searches respectively. The limit parameter will
be silenty trimmed to what is set here. The default is 50 results.

`-query-timeout` sets the number of seconds after which a query to the
database will be canceled and an error returned to the user.

Photon has limited support for synonyms. Check out the
[synonym documentation](/docs/synonyms.md) for more information.

### Importing data from Nominatim

If you want to have a database with a different set of data than provided by
the dumps, you can create a photon database by importing the data from
a Nominatim instance. To learn how to set up a Nominatim database, refer
to the [installation documentation](https://nominatim.org/release-docs/latest/admin/Installation/).

If you haven't already set a password for your Nominatim database user,
do it now (change user name and password as you like, below):

```
psql -d nominatim -c "ALTER USER nominatim WITH ENCRYPTED PASSWORD 'mysecretpassword'"
```

Import the data to photon:

```
java -jar photon-*.jar -nominatim-import -host localhost -port 5432 -database nominatim -user nominatim -password mysecretpassword -languages es,fr
```

The import of worldwide data set takes about half a day. SSD/NVME disks are
recommended to accelerate Nominatim queries.

There are a couple of parameters that influence which kind of data will be
imported:

`-languages <languages>` states which languages _in addition to the local name_
are added to the database. Give the languages as a comma-separated list of
language codes. Note that a very long list of languages will slow down queries.
This is a known restriction we are working on. You can still import a large list
but then might need to restrict the languages that are actively used when starting
the API service.

`-country-codes` allows to filter the data to be imported by country. Set this
to a comma-separated list of two-letter language codes.

`-extra-tags` defines a set of tags to add from Nominatim's
[extratags](https://nominatim.org/release-docs/latest/customize/Import-Styles/#advanced-main-tag-handling) column.
Either give a comma-separated list of OSM tags to include or set the special
keyword `ALL` to unconditionally include all extra tags. The default is to
include nothing.

When `-import-geometry-column` is set, photon will not only include centroid
and bounding box in results but the full geometry as well. **WARNING:**
enabling this option will more than double the size of the database.
(_Experimental Feature!_)

### Updating data via Nominatim

Once you have imported your own photon database from a Nominatim source, you
can keep up-to-date with the source through regular updates.

First prepare the Nominatim database with the appropriate triggers:

```
java -jar photon-*.jar -database nominatim -user nominatim -password ... -nominatim-update-init-for update_user
```

This script must be run with a user that has the right to create tables,
functions and triggers.

'update-user' is the PostgreSQL user that will be used when updating the
Photon database. The user needs to already have read rights on the database.
It also needs to be able to update the status table. The necessary
update rights will be granted during initialisation.

Now you can run updates on Nominatim using the usual methods as described
in the [documentation](https://nominatim.org/release-docs/latest/admin/Update/).
To bring the Photon database up-to-date, make sure the Nominatim updates
are not running and then run the Photon update process:

```
java -jar photon-*.jar -nominatim-update -database nominatim -user nominatim -password ...
```

Alternatively, you can trigger updates via a special API endpoint. To be able
to do so, you need to run photon with the update API enabled:

```
java -jar photon-*.jar -enable-update-api -database nominatim -user nominatim -password ...
```

The update is then triggered like this:

```
curl http://localhost:2322/nominatim-update
```

This will trigger a single updates run. If another update is already in progress,
the request returns with an error. You can also check if the updates have
finished by using the status API:

```
curl http://localhost:2322/nominatim-update/status
```

It returns a single JSON string: "BUSY" when updates are in progress,
or "OK" when another update round can be started.

For your convenience, this repository contains a script to continuously
update both Nominatim and Photon using Photon's update API. Make sure you
have Photon started with -enable-update-api and then run:

```
export NOMINATIM_DIR=/srv/nominatim/...
./continuously_update_from_nominatim.sh
```

where NOMINATIM_DIR is the project directory of your Nominatim installation.

### Exporting data to a JSON dump

Imports normally write the data directly into a ElasticSearch/OpenSearch
database. When adding the parameter `-json <filename>` the data is written
out as a newline-delimited JSON dump file instead. All parameter that influence
a database import are valid for a JSON dump as well. To dump the data to
standard output (for example, to directly pack the data), use `-json -`.

### Importing data from a JSON dump

To read a dump file previously created with photon, use the following command:

```
java -jar photon-*.jar -nominatim-import -import-file <filename>
```

When the filename is `-`, then photon reads from standard input allowing you
to unpack dump files on the fly.

The file import accepts all the parameters of an import from a Nominatim database.
That means that you can create a dump file of a full Nominatim database and then
later import it into different photon databases with different filtering
optins like `-country-code`, `-language` or `-extra-tags`.

## Photon API

photon has three default endpoints: `/api` for forward search, `/reverse` for
reverse geocding and `/status` as a health check of the server.

For the `/structured` endpoint for structured queries, see
[docs/structured.md](docs/structured.md). This endpoint is not available
on the public demo server.

The `/update` endpoint for triggering updates is described in the section
"Updating data via Nominatim" above.

### Search

A simple forward search for a place looks like this:

```
http://localhost:2322/api?q=berlin
```

#### Location Bias

```
http://localhost:2322/api?q=berlin&lon=10&lat=52&zoom=12&location_bias_scale=0.1
```

There are two optional parameters to influence the location bias. 'zoom'
describes the radius around the center to focus on. This is a number that
should correspond roughly to the map zoom parameter of a corresponding map.
The default is `zoom=16`.

The `location_bias_scale` describes how much the prominence of a result should
still be taken into account. Sensible values go from 0.0 (ignore prominence
almost completely) to 1.0 (prominence has approximately the same influence).
The default is 0.2.

#### Filter results by bounding box

```
http://localhost:2322/api?q=berlin&bbox=9.5,51.5,11.5,53.5
```

The expected format for the bounding box is minLon,minLat,maxLon,maxLat.

### Reverse

The basic lookup of a coordinate looks like this:

```
http://localhost:2322/reverse?lon=10&lat=52&radius=10
```

The optional radius parameter can be used to specify a value in kilometers
to reverse geocode within. The value has to be between 0 and 5000 km.

### Parameters common to Search and Reverse

The following parameters work for search, reverse search and
structured search.

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

#### Filter results by category

Use `include` and `exclude` parameters to filter by category. What
categories are defined depends on the specific installation of Photon.

See the [category documentation](docs/categories.md) for more information.

#### Dedupe results

```
http://localhost:2322/api?q=berlin&dedupe=1
```

Sometimes you have several objects in OSM identifying the same place or object in reality.
The simplest case is a street being split into many different OSM ways due to different characteristics.
Photon will attempt to detect such duplicates and only return one match.
Setting `dedupe` parameter to `0` disables this deduplication mechanism and ensures that all results are returned.
By default, Photon will attempt to deduplicate results which have the same `name`, `postcode`, and `osm_value` if exists.

### Results for Search and Reverse

Photon returns a response in [GeocodeJson format](https://github.com/geocoders/geocodejson-spec/tree/master/draft)
with the following extra fields added:

* `extra` is an object containing any extra tags, if available.

Example response:

```
json
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

### Status

```
http://localhost:2322/status
```

returns a JSON document containing the status and the last update date of
the data. (That is the date, when the data is from, not when it was imported
into photon.)

## Contributing

All code contributions and bug reports are welcome.

For questions please either use [Github discussions](https://github.com/komoot/photon/discussions) or join the [OpenStreetMap forum](https://community.openstreetmap.org/).

## License

photon software is open source and licensed under [Apache License, Version 2.0](https://opensource.org/licenses/Apache-2.0)

## Related Projects

- photon's search configuration was developed with a specific test framework. It is written in Python and [hosted separately](https://github.com/yohanboniface/osm-geocoding-tester).
- There is a [leaflet-plugin](https://github.com/komoot/leaflet.photon) for displaying a search box with a photon server in the backend.

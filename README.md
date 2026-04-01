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

## Installation

### Requirements

photon requires Java, version 21+.

If you want to run against an external database instead of using the embedded
server, OpenSearch 3.x is needed.

A planet-wide database requires about 95GB disk space (as of 2026, grows by
about 10% a year). Using SSDs for storage is strongly recommended, NVME would
even be better. At least 64GB RAM are recommended for smooth operations, more,
if the server takes significant load. Running photon with less RAM is possible
but consider increasing the available heap (using `java -Xmx8G -jar ...` for
example). Be careful to make sure that there remains enough free RAM that the
system doesn't start swapping.

If you want to import data from Nominatim, there are additional
[requirements for Nominatim](https://nominatim.org/release-docs/latest/admin/Installation/#prerequisites).

### Setting photon up with the release binaries and extracts

This is the easiest way to set up a self-hosted photon instance.
Pre-built jar files can be downloaded from the
[Github release page](https://github.com/komoot/photon/releases/latest).

[GraphHopper](https://www.graphhopper.com/) kindly provides weekly updated
dumps of the photon database at
[https://download1.graphhopper.com/public](https://download1.graphhopper.com/public).
The dumps are available for the world-wide dataset and for selected country datasets.
The dumps contain names in English, German, French and local language. There
is no support for
[full geometry output](https://github.com/komoot/photon/pull/823). If you need
this feature, you need to import your own database from a JSON dump.

Follow the instruction on the webpage to find the right dump suitable for
your version.

Make sure you have bzip2 or pbzip2 installed. Do not use WinRAR for unpacking,
it is known to have issues with the files. Execute one of these two
commands in your shell to download, uncompress and extract the huge
database in one step:

```
wget -O - https://download1.graphhopper.com/public/photon-db-planet-1.0-latest.tar.bz2 | bzip2 -cd | tar x
# you can significantly speed up extracting using pbzip2 (recommended):
wget -O - https://download1.graphhopper.com/public/photon-db-planet-1.0-latest.tar.bz2 | pbzip2 -cd | tar x
```

Don't forget to adapt the directory to **match your photon version**.

#### Updating photon with a new version of the database dump

When you want to update your local database with a newer version of the
database dump, then you need to swap out the databases atomically:

* download and unpack the new version
* swap the directories to put the new directory in place of the old one
* restart photon and make sure everything works as expected
* delete the old database

This unfortunately means you need twice the space of the database for updates.

_WARNING: Never unpack the database in place of the old one. This will lead
to corrupted data._


## Usage

Change to the directory where the `photon_data` database directory is located
(aka the parent directory of `photon_data`). Then start photon with the
following command:

```
java -jar photon-*.jar serve
```

The webserver is then available at `http://localhost:2322`.

For a full documentation of the usage, including on how to import and
update a database, see the [Usage documentation](docs/usage.md).


## Photon API

A full description of the Photon API and results can be found in the
[API documentation](docs/api-v1.md).

## Building photon from scratch

photon uses gradle for building. To build the package from source make
sure you have a JDK installed. Then run:

```
./gradlew build
```

This will build and test photon. The final jar can be found in the `target` directory.

## Audit Logging

By default incoming requests are not logged. To enable logging of all incoming requests via
the API, use the parameter `-enable-audit-log`.
Requests used for (reverse)geocoding will not contain the actual query, unless the additional parameter `-audit-log-no-privacy` is set.

## Contributing

Code contributions and bug reports are welcome.

PRs that include AI-generated content, may that be in code, in the PR
description or in documentation need to

1. clearly mark the AI-generated sections as such, for example, by
   mentioning all use of AI in the PR description, and
2. include proof that you have run the generated code on an actual
   installation of photon. Adding and executing tests will not be
   sufficient. You need to show that the code actually solves the problem
   the PR claims to solve.

For questions please either use [Github discussions](https://github.com/komoot/photon/discussions)
or join the [OpenStreetMap forum](https://community.openstreetmap.org/).

## License

photon is open source and licensed under [Apache License, Version 2.0](https://opensource.org/licenses/Apache-2.0)

## Related Projects

- photon's search configuration was developed with a specific test framework. It is written in Python and [hosted separately](https://github.com/yohanboniface/osm-geocoding-tester).
- There is a [leaflet-plugin](https://github.com/komoot/leaflet.photon) for displaying a search box with a photon server in the backend.

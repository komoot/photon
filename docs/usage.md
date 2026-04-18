# photon Usage

This section describes the usage of the photon command-line tool. photon
follows the philosophy of git-like subcommands. Commands available are:
import, update-init, update, dump-nominatim-db and serve.

To get a detailed overview of the parameters available for each command
use

    java -jar photon.jar <COMMAND> -h


## Configuring the photon Database

photon can either be run in embedded mode or use an external OpenSearch
database for storage.

The embedded mode is the default mode of operation. photon starts a
private instance of OpenSearch. This mode is the most easy to use and enough
for most small and medium-sized installations. The database in embedded mode
is saved in a directory called `photon_data`. The `**-data-dir** parameter
can be used to change where this directory should be located. The default
is to use the current working directory.

For large installation where scalability is required, photon can also be run
against an external OpenSearch instance. This can be in particular be useful
when you want to configure multiple distributed nodes. Refer to the
[OpenSearch documentation](https://docs.opensearch.org/latest/about/)
to learn how to set up an OpenSearch instance. OpenSearch 3.x is
required. Use the **-transport-addresses** parameter to point photon to the
OpenSearch nodes. It takes a comma-separated list of node addresses.

photon uses to the 'photon' cluster by default. This can be changed with the
**-cluster** parameter if necessary.

## Running a photon server

When you already have a database, either because you downloaded a database
dump from the [export server](https://download1.graphhopper.com/public/) or
because you have imported your own database (see below), then you can
start a photon server with the `serve` command.

photon will be available at 'http://127.0.0.1:2322' by default. This can be
changed with the parameters **-listen-ip** and **-listen-port**. For a
description of the API, see the [API documentation](api-v1.md).

To enable CORS (cross-site requests), use the switch **-cors-any** to allow
requests from any origin or set the parameter **-cors-origin** to one or
more specific origins. By default, CORS support is disabled.

**-max-results** (for '/api' searches) and **-max-reverse-results**
(for '/reverse' searches) can be used to change the maximum
number of results that may be requested with the API `limit` parameter.
The limit parameter will be silently trimmed to what is set here.
The default is 50 results for both endpoints.

**-default-language** allows to change in which language results are returned
when the user hasn't explicitly (or implicitly via the Accept-Language header)
chosen a language. The default is to return all results in the local language.

To protect against bogus user queries, photon sets a maximum execution time
for each query to the OpenSearch instance. The default is 7 seconds and can
be changed with the **-query-timeout** parameter.

photon has limited support for synonyms. Check out the
[synonym documentation](synonyms.md) for more information.

If you plan to monitor the server, photon can export metrics about
queries and health. Use the **-metrics-enable** parameter to enable
a '/metrics/' endpoint that exposes these statistics. Right now, only
[prometheus](https://prometheus.io/) format is implemented.

## Importing data

The `import` command creates a new database from either a Nominatim
database or from a JSON dump file. This always creates a new photon database
from scratch. Any existing database is silently deleted.

The import of a world-wide data set takes about half a day. SSD/NVME disks are
strongly recommended. There are reports that running photon from spinning disks
leads to cryptic errors in OpenSearch.

### Importing from a Nominatim database

The default mode is to import the database from a Nominatim PostgreSQL data.
To learn about Nominatim and how to set up a database refer
to the [installation documentation](https://nominatim.org/release-docs/latest/admin/Installation/).

_Important: make sure that updates are stopped on the Nominatim database
when running the photon import or results are unpredictable._

photon will try to connect to a PostgreSQL server in the default location
(localhost at port 5432) using the user 'nominatim' and look for a database
'nominatim'. You can customize this with the parameters **-host**, **-port**,
**-user**, **-password**, and **-database**.

You likely will need to enable password authentication to the PostgreSQL database.
To do so, you can set a password for the database user like this:

    psql -d nominatim -c "ALTER USER www-data WITH ENCRYPTED PASSWORD 'mysecretpassword'"

The PostgreSQL user only needs read access to the query tables of the
Nominatim database (similar to
[Nominatim's web user](https://nominatim.org/release-docs/latest/customize/Settings/#nominatim_database_webuser)).
However, there is one exception. photon needs a special index on the placex
table which it tries to create when it doesn't exist yet. Either make sure
that the user has the right to create indexes or create the index manually
before starting the import with the following command:

    psql -d nominatim -c 'CREATE INDEX ON placex(country_code)'

Adapt the database name as required.

### Importing from a dump file

To load the photon database from a dump file (for example from the
[export server](https://download1.graphhopper.com/public/)), use the
**-import-file** parameter. You can either give it the filename directly
or use '-' to make photon read from standard input. This is particularly
useful when the dump file is packed in some way:

    zstd --stdout -d photon-dump.jsonl.zst | java -jar photon.jar import -import-file -

### Filtering the data to be imported

By default, photon will include all data from the database or file. To
restrict the import to certain countries, use the **-country-codes** parameter.
It requires a comma-separated list of two-letter country codes.

Names of places and in addresses are imported with their local version and
with the translations given by the **-languages** parameter. The default
is to add translations in English, German, French and Italian, where available.
The languages parameter also determines which languages the user can later
request when searching. The languages need to be given in language codes
as per [convention in OpenStreetMap](https://wiki.openstreetmap.org/wiki/Names#Localization).

Both the dumps and a Nominatim database may contain some extra data for the
place objects. When the data is derived from OpenStreetMap data, then the
extra data simply consists of the additional tags of the OSM object. Other
sources may use this for other information. To add this extra data to the
Photon database, use the **-extra-tags** parameter and give it
a comma-separated list of keys to include. You can also use the
special keyword 'ALL' to make it include all available data. The default is
to add no extra data at all.

When available, photon can also import full geometries instead of just a
centroid and bounding box. These geometries will then be returned with the
response. To enable this, use the **-full-geometries** switch.

_Hint: if these filtering options are not sufficient, it is always possible
to preprocess the json dump before feeding it to photon. Have a look at the
[dump spec](json-dump-format-0.1.0.md) to learn about the format of this
file._

### Reverse-only mode

By using the parameter `-reverse-only` the database can be set up, so that
only the `/reverse` endpoint is available. Such a database is significantly
smaller than a fully searchable photon database and also faster to import.

## Updating Data

Updating a photon database can at the moment only be done from a
Nominatim database.

### Preparing the database

Once you have created an initial photon database from the Nominatim source,
the Nominatim database needs to be prepared for updates. photon needs tables
and triggers in order to track updates. Use the `update-init` command to do
that. The command must be run with a user that has the right to change the
database. It takes one mandatory parameter **-import-user**. Use the
parameter to tell photon which user will later run the updates. This user then
gets the appropriate minimal rights to change the update tables.

### Running updates from the command-line

Now you can run updates on the Nominatim database using the usual methods as described
in its [documentation](https://nominatim.org/release-docs/latest/admin/Update/).
To bring the photon database up-to-date use the command `update`.
Make sure Nominatim updates are not running while doing that.

Updates allow the same filter options as imports. If you have used filters
on import, you _must_ repeat these options here or photon will fall back to
the defaults for the updated data.

### Running updates via the API

Updates from the command-line can only be applied when the photon database
is offline. To run updates while serving data at the same time, you need to
enable the `/nominatim-update` endpoint. This can be done by running the
`serve` command (see above) with the switch **-enable-update-api**. Don't
forget to add the appropriate parameters to configure the PostgreSQL connection
and, when applicable, the import filters.

Once enabled, a single update run can be triggered via the API like this:

    curl http://localhost:2322/nominatim-update

If another update is already in progress, the request returns with an error.
Otherwise it will report success immediately and then run the update in the
background.

You can check if the update has finished by using the status call:

    curl http://localhost:2322/nominatim-update/status

It returns a single JSON string "BUSY" when updates are in progress,
or "OK" when another update round can be started.

For your convenience, this repository contains a script to continuously
update both Nominatim and photon using photon's update API. Make sure you
have photon started with -enable-update-api and then run:
 
```
export NOMINATIM_DIR=/srv/nominatim/...
./continuously_update_from_nominatim.sh
```
 
where `NOMINATIM_DIR` is the project directory of your Nominatim installation.

_WARNING: never make the /nominatim-update endpoint available on a public
network. While the endpoint is safe to be triggered by random requests,
updates nonetheless create load on your database and running updates while
Nominatim updates are in progress may lead to inconsistencies in the photon
database. You therefore would want to be able to control when exactly an
update is run._

## Exporting Data to a JSON Dump

The command `dump-nominatim-db` can be used to create a JSON dump file
from a Nominatim database. The required parameter **-export-file** points
to the file to export to. Use '-' to write the dump to standard output.
This can be useful when you want to pack the output directly or filter
it further before saving.

Otherwise the same parameters as for imports apply for configuring the
connection to the PostgreSQL database and filtering the data.

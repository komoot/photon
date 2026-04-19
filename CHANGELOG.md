# Change Log

All notable changes to this project will be documented in this file.
This project adheres to [Semantic Versioning](http://semver.org/).

## [1.1.0] - 2026-04-18

* add special reverse-only mode creating a smaller DB with on search capability
* allow arbitrary json data types in extra fields
* improve handling of postcodes in deduplication
* add countrycode filter to search (thanks @tsamaya)
* add rescoring of the OpenSearch result set (adding rematching against query)
* add postcodes from Nominatim's location_postcodes table to imported data
* various small performance optimisations (thanks @otbuz, @henrik242)
* fix dumping of alternative names like alt_name
* improve scoring of location bias

## [1.0.1] - 2026-03-09

* tolerate adding new properties in future versions

## [1.0.0] - 2026-02-09

* remove ElasticSearch backend
* raise minimum required Java version to 21 (requirement from OpenSearch)
* new git-like command style CLI
* streamline database removing all unnecessary indexes
* move database properties into index metadata
* only update index mapping when synonyms are requested
* manually compute terms for collector fields and add importance scores
* remove language-dependent indexes
* add a special query path for single term queries
* reorganise query code to avoid code duplication between endpoints
* new `dedupe` parameter to suppress result deduplication (thanks @hircumg)
* streamlining of json dump format, version 0.1.0 now finalised
* add categories and category filters
* allow searches without query when category filters are present (thanks @henrik242)
* add Prometheus metrics (thanks @henrik242)
* add query option to always include results with house number in
  result (thanks @henrik242)
* switch to better compression of database indexes
* switch to using JSpecify for nullable annotations
* clean up use of string constants
* improve handling of street-less addresses
* various improvements to documentation


## [0.7.4] - 2025-09-18

* fix regression with computation of location bias

## [0.7.3] - 2025-08-13

* better error reporting when the database doesn't exist
* ensure that database is properly closed when shutting down

## [0.7.2] - 2025-07-03

* fix inconsistency in naming of 'extra' field

## [0.7.1] - 2025-06-30

* fix overwriting of streets with addr:place
* restore default of returning 1 results for reverse
* restore default of sorting reverse results by distance

## [0.7.0] - 2025-06-01

* make OpenSearch the recommended backend
* export of database as a json dump and reading from a json dump
* rework Nominatim exports to read country by country and cache address info
* support importing of full geometries (thanks @red-fenix)
* fix missing OSM IDs and importance values in photon-opensearch output
* fix startup error when synonym file was wrong
* correctly handle precedence between postcode boundaries and postcode address tags
* move to javalin webframework to replace unmaintained spark
* drop json dependency in favour of jackson
* add ISO3166-2 state codes to recognised address parts

## [0.6.2] - 2025-01-13

* avoid sending too much data to the database during updates

## [0.6.1] - 2024-12-04

* OpenSearch: correctly set timeout for reverse endpoint
* correctly translate 64bit numbers and floating point numbers when reading
  from OpenSearch database
* OpenSearch: dump raw result content when running with debug mode
* move score and importance into debug output
* make sure to reopen the index when installation of synonym fails
* reduce installed and loaded plugins to the necessary minimum
* increases the queue size for the importer

## [0.6.0] - 2024-10-29

* add OpenSearch backend
* add structured search (OpenSearch only, thanks @tobiass-sdl)
* switch build system to gradle
* new configuration parameters for setting the maximum number of
  returned results (thanks @karussell)
* fix importing addresses of objects that are not part of an address
  themselves (e.g. lakes, mountains)
* allow multiple CORS origins on the command line (thanks @burleight)

## [0.5.0] - 2024-03-06

* rework database update process and fix updating of multinumber addresses
* add /nominatim-update/status for monitoring update progress
* add /status endpoint (thanks @ybert)
* add command-line parameter to set the timeout for ElasticSearch queries
* fix issue where Photon couldn't be run from directories with spaces in the name
* fix off-by-one error when expanding interpolations
* remove lombok and replace uses with explicit code
* typo and grammar fixes to log messages and documentation

## [0.4.4] - 2023-12-18

* drop support for Java 8 and add support for Java 21
* documentation improvements (thanks @TKYK93 and @sebix)

## [0.4.3] - 2023-07-28

* add tag filtering for reverse geocoding (thanks @ybert)
* improvements to build process (thanks @otbutz)

## [0.4.2] - 2022-11-19

* adapt import process for changes in linked place handling in Nominatim
* search: disallow skipping of words in queries with one or two words
* do fuzzy matching against all language variants

## [0.4.1] - 2022-08-31

* fix regression where language parameter is no longer respected

## [0.4.0] - 2022-07-30

* add support for synonyms
* disable update endpoint by default
* add layer filter parameter (thanks @macolu)
* compatibility fixes for the upcoming version 4.1 of Nominatim
* log4j JndiLookup class removed for security reasons
* fix duplicate call to completeDoc on updates
* add sanity checks for housenumbers
* remove dependencies to jackson-jaxrs, common-io, guava and nv-i18n
* ensure import thread is always terminated (thanks @codepainters)
* general code cleanup and reorganization (thanks @tahini)

## [0.3.5] - 2021-07-12

* reorganise ES queries to allow for multi-lingual search
* add zoom parameter for location bias
* save database properties in database
* add importing of extra tags
* output importance and ES score in debug mode
* improve import speed for POI objects (thanks to @alfmarcua)
* code cleanup and modernisation
* extend test suite
* improve startup of internal ES server (thanks @trafficant)
* convert included website to static site
* demo server switched to https://photon.komoot.io

## [0.3.4] - 2020-09-13

* fix missing state field and add county to output
* fix missing country code due to incompatible library change
* adapt to new location of linking information in Nominatim
* make popups on website readable

## [0.3.3] - 2020-07-08

* rework address computation
* new command-line option for running updates
* add new fields: county, district, locality and type
* allow searching for house name
* respect Accept-Language header
* make default language configurable (default is now to return results
  in local language)

## [0.3.2] - 2019-11-29

* new options for enabling CORS headers (thanks @simonpoole)
* new bbox API parameter (thanks @hbruch, @Gerungofulus)
* output country codes (thanks @Gerungofulus)
* new debug flag for debugging ES queries (thanks @simonpoole)
* improve dependency handling (thanks @simonpoole)
* add support for Java 11
* add API-level tests next to existing unit tests (thanks @simonpoole)

## [0.3.1] - 2019-03-08

* respect -cluster setting also when running standalone
* remove diacritics from words (thanks @systemed)
* set distance_sort default to true for ReverseRequest (thanks @brugger-trafficon)
* add osm_value to dedupe key (thanks @hbruch)
* fix scripts for continuous updates from Nominatim
* ensure compatibility with latest Nominatim version

## [0.3.0] - 2018-04-14

* ensure compatibility with Nominatim 3.1.0
* improve performance of reverse geocoding
* improve house number handling (interpolations, house numbers with hyphens)
* improve algorithm for computing location bias
* new country code filter for export
* new parameter limit and radius for reverse geocoding

## [0.2.2] - 2015-03-06

* allow searching for OSM tags

## [0.2.0] - 2015-01-16

* state information is returned #102
* test framework working
* updated to latest ElasticSearch version 1.3.2
* disable dynamic mapping of ElasticSearch
* add regional name to index
* dynamic language
* code cleanup and documentation

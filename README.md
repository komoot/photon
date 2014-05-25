# photon

_Photon_ is an open source geocoder built for [OpenStreetMap](http://www.osm.org) data. It is based on [elasticsearch](http://elasticsearch.org/) - an efficient, powerful and highly scalable search platform.

_Photon_ was started by [komoot](http://www.komoot.de) and provides search-as-you-type and multilingual support. It's used in production with thousands of requests per minute at [www.komoot.de](http://www.komoot.de).

The current version is still under heavy development, feel free to test and participate. The previous version based on solr is accessible in the [deprecated solr branch](https://github.com/komoot/photon/tree/deprecated-solr-version).

### Features
- high performance
- highly scalability
- search-as-you-type
- multilingual search
- location bias
- typo tolerance
- OSM data import (built upon [Nominatim](https://github.com/twain47/Nominatim)) inclusive continuous updates

### Prerequisites
  - Java 6
  - Maven
  - Python 2/3 (currently necessary for API)
  - [Nominatim](https://github.com/twain47/Nominatim) (currently necessary for continuous updates)

### Installation
```bash
git clone git@github.com:komoot/photon.git
cd photon
mvn clean package
```

## Quick Start

### Import Data
To import worldwide data in four languages (English, German, French and Italian) you can use our preprocessed
dataset. You won't be able to continuously update your data to keep them in sync with the latest OSM changes. However
you avoid to install and import Nominatim which is time consuming.

```bash
# important: we do not yet provide this dump, creation will be finished soon
java -jar target/photon-0.1-SNAPSHOT.jar -import-snapshot http://photon.komoot.de/data/world.zip
```
Be aware that you download several GB of data, the import itself will take only a few minutes.


### Import Data (inclusive continuous updates)
If you need continuous updates or want to import country extracts only, you need to install Nominatim by yourself. Once
you have your nominatim database ready, you can import the data to photon:

```bash
java -jar target/photon-0.1-SNAPSHOT.jar -nominatim-import -host localhost -port 5432 -database nominatim -user nominatim -password ...
```

The import will take some hours/days, ssd disk are recommended to accelerate nominatim queries.

For continuous updates you can run [continuously_update_from_nominatim.sh](./continuously_update_from_nominatim.sh):
```bash
export NOMINATIM_DIR=/home/nominatim/...
./continuously_update_from_nominatim.sh
```

### Start Photon
```bash
java -jar target/photon-0.1-SNAPSHOT.jar
```

## Detailed Usage

### Search API
#### Start Photon
```bash
java -jar target/photon-0.1-SNAPSHOT.jar
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

### create snapshot
You can create a photon snapshot that allows you transfer data from one photon instance to another. This way you can import your data
once (which might take quite a lot of time) and reimport it very quickly on other machines. It is also useful for backups.

```bash
java -jar target/photon-0.1-SNAPSHOT.jar -create-snapshot photon_snapshot_2014_05
```

The snapshot will be stored in ``photon_data/dumps/photon_snapshot_2014_05.zip```.


### import snapshot
** this feature is still in development **

You can reimport a snapshot with:
```bash
java -jar target/photon-0.1-SNAPSHOT.jar -import-snapshot file:///home/photon/src/photon/photon_data/dumps/photon_snapshot_2014_05.zip
```
Caution: all previous data will be lost! You can also import remote files (e.g. ```http://example.com/photon.zip```).


### delete index
To delete all data run
```bash
java -jar target/photon-0.1-SNAPSHOT.jar -delete-index
```
Photon will be started with an empty index.


### Run the Demo UI

The python demo UI is located in `website/photon`.

It has been developed with python3.4 (but should work with python2.x). We suggest to use virtualenv for the installation.

* Get the virtualenv system packages:
  ```
  sudo apt-get install python-pip python-virtualenv virtualenvwrapper
  ```
* Create a virtualenv:
 ```
 mkvirtualenv -p python3.4 photon
 ```
* Install dependencies:
 ```
 cd website/photon
 pip install -r requirements.txt
 ```
* Run the server
 ```
 python app.py
 ```
* Go to http://localhost:5001/ and test it!

## Metrics

Photon comes with a python suite to test search relevance.

### Running

First, install `pytest` if not already installed:

    pip install pytest

then:

    cd test
    py.test

For a global help, type:

    py.test -h

Tests are split by geographical area, so you can run only a subset of all the tests,
for example because your local database only contains a small area, or because you want
to focus on some data.

Available subsets: `germany`, `france`, `iledefrance`, `italy`.

If you want to run only a subset of the tests, run for example

    py.test -m iledefrance

What if I want to have details about the failures?

    py.test --tb short

How can I stop at first failing test?

    py.test -x

Can I change the photon URL I'm testing against?

    py.test --photon-url http://photon.komoot.de/api/

### Adding metrics

We support python, CSV and YAML format.

Before creating a new file, check that there isn't a one that can host the test
you want to add.

*How do I name my file?* Just make it start with `test_`, and chose the right
extension according to the format you want to use: `.py`, `.csv` or `.yml`.

*Where do I save my file?* Chose the right geographical area, and if you create
a new area remember to create all levels, like `france/iledefrance/paris`.

Remember to check the test already done to get inspiration.

#### Python

They are normal python tests. Just check that you have two utils in `base.py`:
`search` and `assert_search` that can do a lot for you.

#### CSV

One column are mandatory: `query`, where you store the query you make.
Then you can add as many `expected_xxx` columns you want, according to what
you want to test. For example, to test the name in the result, you will store
the expected value in the column `expected_name`; for an `osm_id` it will be
`expected_osm_id`, and so on.
Optional columns:
* `limit`: decide how many results you want to look at for finding your result
(defaul: 1)
* `lat`, `lon`: if you want to add a center for the search
* `comment`: if you want to take control of the ouput of the test in the
command line
* `skip`: add a `skip` message if you want a test to be always skipped (feature
not supported yet for example)

#### YAML

The spec name is the query, then one key is mandatory: `expected`, which then
has the subkeys you want to test against (`name`, `housenumber`…).
Optional keys: `limit`, `lang`, `lat` and `lon`, `skip`.
You can add categories to your test by using the key `mark` (which expects a
list), that you can then run with `-m yourmarker`.


## Licence
_Photon_ software is open source and licensed under [Apache License, Version 2.0](http://opensource.org/licenses/Apache-2.0)

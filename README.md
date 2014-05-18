# photon

_Photon_ is an open source geocoder built for [OpenStreetMap](http://www.osm.org) data. It is based on [elasticsearch](http://elasticsearch.org/) - an efficient, powerful and highly scalable search platform.

_Photon_ was started by [komoot](http://www.komoot.de) and provides search-as-you-type and multilingual support. It's used in production with thousands of requests per minute at [www.komoot.de](http://www.komoot.de).

The current version is still under heavy development, feel free to test and participate.

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

## Usage

### Import Data
To import worldwide data in four languages (English, German, French and Italian) you can use our preprocessed
dataset. You won't be able to continuously update your data to keep them in sync with the latest OSM changes. However
you avoid to install and import Nominatim which is time consuming.

Simply start photon
```bash
java -jar target/photon-importer-0.1-SNAPSHOT.jar
```

and load the global dataset by calling
```
curl http://localhost:4567/dump/import # not working yet!
```
Be aware that you download several GB of data, the import itself will take only a few minutes.

### Import Data (inclusive continuous updates)
If you need continuous updates or want to import country extracts only, you need to install Nominatim by yourself. Once
this is done you can start the data importer:

```bash
java -jar target/photon-importer-0.1-SNAPSHOT.jar -nominatim-import -host localhost -port 5432 -database nominatim -user nominatim -password ...
```

The import will take some hours/days, ssd disk are recommended to accelerate nominatim queries.

TODO: missing docu for continuous updates

### Start Photon
```bash
java -jar target/photon-importer-0.1-SNAPSHOT.jar
```

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

## Licence
_Photon_ software is open source and licensed under [Apache License, Version 2.0](http://opensource.org/licenses/Apache-2.0)

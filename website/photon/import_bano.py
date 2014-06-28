#!/usr/bin/env python

import csv
import sys
import os
from elasticsearch import Elasticsearch
from elasticsearch.helpers import bulk_index

ES = Elasticsearch()
INDEX = 'photon'
DOC_TYPE = 'place'
FILEPATH = os.environ.get('BANO_FILEPATH', 'bano.csv')


"""
{
  "osm_id": 2525485803,
  "osm_type": "N",
  "osm_key": "place",
  "osm_value": "house",
  "importance": 0.0,
  "coordinate": {
    "lat": 48.6351388,
    "lon": 2.4480519
  },
  "housenumber": "3",
  "postcode": "91000",
  "city": {
    "default": "Évry",
    "fr": "Évry"
  },
  "country": {
    "de": "Frankreich",
    "it": "Francia",
    "default": "France",
    "fr": "France",
    "en": "France"
  },
  "street": {
    "default": "Rue du Château"
  },
  "context": {
    "it": "Isola di Francia",
    "default": "Île-de-France, Résidence du Petit Bois, Essonne, Évry",
    "fr": "Île-de-France, Essonne",
    "en": "Ile-de-France"
  }
}
"""

fields = [
    'source_id', 'housenumber', 'street', 'postcode', 'city', 'source', 'lat',
    'lon', 'dep', 'region'
]


def row_to_doc(row):
    context = ', '.join([row['dep'], row['region']])
    return {
        "osm_id": 123456789,  # Fix me when we have a source_id
        "osm_type": "N",
        "osm_key": "place",
        "osm_value": "house",
        "importance": 0.0,
        "coordinate": {
            "lat": row['lat'],
            "lon": row['lon']
        },
        "housenumber": row['housenumber'],
        "postcode": row['postcode'],
        "city": {
            "default": row['city'],
            "fr": row['city']
        },
        "country": {
            "de": "Frankreich",
            "it": "Francia",
            "default": "France",
            "fr": "France",
            "en": "France"
        },
        "street": {
            "default": row['street'],
        },
        "context": {
            "default": context,
            "fr": context,
        }
    }


def cleanup():
    query = {
        "query": {
            "filtered": {
                "query": {"match_all": {}},
                "filter": {"exists": {"field": "housenumber"}}
            }
        }
    }
    ES.delete_by_query(
        index=INDEX,
        doc_type=DOC_TYPE,
        body=query
    )

if __name__ == "__main__":
    # first cleanup the housenumber data (we don't want duplicates)
    cleanup()
    with open(FILEPATH) as f:
        reader = csv.DictReader(f, fieldnames=fields)
        count = 0
        data = []
        for row in reader:
            data.append(row_to_doc(row))
            count += 1
            if count % 100000 == 0:
                print('Start indexing batch of', len(data))
                bulk_index(ES, data, index=INDEX, doc_type=DOC_TYPE, refresh=True)
                print('End indexing of current batch')
                sys.stdout.write("done {}\n".format(count))
                data = []

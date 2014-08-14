#!/usr/bin/env python

import csv
import sys
import os
import json
from elasticsearch import Elasticsearch
from elasticsearch.helpers import bulk_index

ES = Elasticsearch()
INDEX = 'photon'
DOC_TYPE = 'place'
FILEPATH = os.environ.get('BANO_FILEPATH', 'bano.csv')
DUMPPATH = os.environ.get('BANO_DUMPPATH', '/tmp')

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
                "query": {
                    "match_all": {}
                },
                "filter": {
                    "and": {
                        "filters": [
                            {
                                "exists": {
                                    "field": "housenumber"
                                }
                            },
                            {
                                "term": {
                                    "osm_value": "house"
                                }
                            }
                        ]
                    }
                }
            }
        }
    }
    ES.delete_by_query(
        index=INDEX,
        doc_type=DOC_TYPE,
        body=query
    )


def index(data):
    print('Start indexing batch of', len(data))
    bulk_index(ES, data, index=INDEX, doc_type=DOC_TYPE, refresh=True)
    print('End indexing of current batch')


def dump(data, idx):
    path = os.path.join(
        DUMPPATH,
        'bano_dump_{}'.format(idx)
    )
    with open(path, mode='w', encoding='utf-8') as f:
        f.write('\n'.join(data))
        sys.stdout.write('Dump {0}\n'.format(path))


if __name__ == "__main__":
    # first cleanup the housenumber data (we don't want duplicates)
    cleanup()
    with open(FILEPATH) as f:
        reader = csv.DictReader(f, fieldnames=fields)
        count = 0
        data = []
        idx = 0
        for row in reader:
            data.append('{"index": {}}')
            data.append(json.dumps(row_to_doc(row)))
            count += 1
            if count % 100000 == 0:
                dump(data, idx)
                idx += 1
                data = []
                sys.stdout.write("Done {}\n".format(count))
        if data:
            dump(data, idx)
            sys.stdout.write("Done {}\n".format(count))

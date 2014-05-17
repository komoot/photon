import time
import json
from elasticsearch import Elasticsearch
from elasticsearch.exceptions import NotFoundError


def init_elasticsearch(index):
    es = Elasticsearch()
    try:
        es.indices.delete(index)
        print('Deleted existing index:', index)
    except NotFoundError:
        pass

    with open("mappings.json") as f:
        mappings = json.load(f)

    with open("index_settings.json") as f:
        index_settings = json.load(f)

    es.indices.create(index, body={'mappings': mappings, 'settings': {'index': index_settings}})
    print('index created:', index)
    es.indices.put_alias(index, body={
        "actions": [{
            "add": {
                "index": index,
                "alias": "photon"
            }
        }]
    })


if __name__ == "__main__":
    init_elasticsearch("photon_" + time.strftime("%Y-%m-%d"))

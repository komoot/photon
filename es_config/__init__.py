import time
import json

from pathlib import Path

from elasticsearch import Elasticsearch
from elasticsearch.exceptions import NotFoundError


def init_elasticsearch(index):
    es = Elasticsearch()
    try:
        es.indices.delete(index)
        print('Deleted existing index:', index)
    except NotFoundError:
        pass

    directory = Path(__file__).parent
    with directory.joinpath("mappings.json").open() as f:
        mappings = json.load(f)

    with directory.joinpath("index_settings.json").open() as f:
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

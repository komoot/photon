from elasticsearch import Elasticsearch
from elasticsearch.exceptions import NotFoundError


def init_elasticsearch(index):
    es = Elasticsearch()
    try:
        es.indices.delete(index)
        print('Deleted existing index:', index)
    except NotFoundError:
        pass
    mappings = {
        'place': {
            "_boost": {"name": "importance", "null_value": 0.0},
            "_id": {"path": "id"},
            'properties': {
                'coordinate': {"type": "geo_point"},
                'osm_id': {"type": "long", "index": "not_analyzed"},

                'osm_key': {"type": "string", "index": "no"},
                'osm_value': {"type": "string", "index": "no"},

                'street': {
                    "type": "object",
                    "properties": {
                        "default": {"type": "string", "index": "no",
                                    "copy_to": ["collector.en", "collector.de", "collector.fr", "collector.it"]},
                        "en": {"type": "string", "index": "no", "copy_to": ["collector.en"]},
                        "de": {"type": "string", "index": "no", "copy_to": ["collector.de"]},
                        "fr": {"type": "string", "index": "no", "copy_to": ["collector.fr"]},
                        "it": {"type": "string", "index": "no", "copy_to": ["collector.it"]},
                    }
                },
                'housenumber': {"type": "string", "index": "not_analyzed"},
                'postcode': {"type": "string", "index": "no", "store": True,
                             "copy_to": ["collector.en", "collector.de", "collector.fr", "collector.it"]},

                'name': {
                    "type": "object",
                    "properties": {
                        "default": {"type": "string", "analyzer": "stringanalyser", "fields": {
                            "raw": {"type": "string", "index_analyzer": "raw_stringanalyser",
                                    "search_analyzer": "raw_stringanalyser"}},
                                    "copy_to": ["collector.en", "collector.de", "collector.fr", "collector.it"]},
                        "en": {"type": "string", "analyzer": "stringanalyser", "fields": {
                            "raw": {"type": "string", "index_analyzer": "raw_stringanalyser",
                                    "search_analyzer": "raw_stringanalyser"}}, "copy_to": ["collector.en"]},
                        "de": {"type": "string", "analyzer": "stringanalyser", "fields": {
                            "raw": {"type": "string", "index_analyzer": "raw_stringanalyser",
                                    "search_analyzer": "raw_stringanalyser"}}, "copy_to": ["collector.de"]},
                        "fr": {"type": "string", "analyzer": "stringanalyser", "fields": {
                            "raw": {"type": "string", "index_analyzer": "raw_stringanalyser",
                                    "search_analyzer": "raw_stringanalyser"}}, "copy_to": ["collector.fr"]},
                        "it": {"type": "string", "analyzer": "stringanalyser", "fields": {
                            "raw": {"type": "string", "index_analyzer": "raw_stringanalyser",
                                    "search_analyzer": "raw_stringanalyser"}}, "copy_to": ["collector.it"]},

                        "alt": {"type": "string", "analyzer": "stringanalyser", "fields": {"raw": {"type": "string", "index_analyzer": "raw_stringanalyser", "search_analyzer": "raw_stringanalyser"}},
                                "copy_to": ["collector.en", "collector.de", "collector.fr", "collector.it"]},
                        "int": {"type": "string", "analyzer": "stringanalyser", "fields": {"raw": {"type": "string", "index_analyzer": "raw_stringanalyser", "search_analyzer": "raw_stringanalyser"}},
                                "copy_to": ["collector.en", "collector.de", "collector.fr", "collector.it"]},
                        "loc": {"type": "string", "analyzer": "stringanalyser", "fields": {"raw": {"type": "string", "index_analyzer": "raw_stringanalyser", "search_analyzer": "raw_stringanalyser"}},
                                "copy_to": ["collector.en", "collector.de", "collector.fr", "collector.it"]},
                        "old": {"type": "string", "analyzer": "stringanalyser", "fields": {"raw": {"type": "string", "index_analyzer": "raw_stringanalyser", "search_analyzer": "raw_stringanalyser"}},
                                "copy_to": ["collector.en", "collector.de", "collector.fr", "collector.it"]},
                    }
                },

                'city': {
                    "type": "object",
                    "properties": {
                        "default": {"type": "string", "index": "no",
                                    "copy_to": ["collector.en", "collector.de", "collector.fr", "collector.it"]},
                        "en": {"type": "string", "index": "no", "copy_to": ["collector.en"]},
                        "de": {"type": "string", "index": "no", "copy_to": ["collector.de"]},
                        "fr": {"type": "string", "index": "no", "copy_to": ["collector.fr"]},
                        "it": {"type": "string", "index": "no", "copy_to": ["collector.it"]},
                    }
                },

                'country': {
                    "type": "object",
                    "properties": {
                        "default": {"type": "string", "index": "no",
                                    "copy_to": ["collector.en", "collector.de", "collector.fr", "collector.it"]},
                        "en": {"type": "string", "index": "no", "copy_to": ["collector.en"]},
                        "de": {"type": "string", "index": "no", "copy_to": ["collector.de"]},
                        "fr": {"type": "string", "index": "no", "copy_to": ["collector.fr"]},
                        "it": {"type": "string", "index": "no", "copy_to": ["collector.it"]},
                    }
                },

                'context': {
                    "type": "object",
                    "properties": {
                        "default": {"type": "string", "index": "no",
                                    "copy_to": ["collector.en", "collector.de", "collector.fr", "collector.it"]},
                        "en": {"type": "string", "index": "no", "copy_to": ["collector.en"]},
                        "de": {"type": "string", "index": "no", "copy_to": ["collector.de"]},
                        "fr": {"type": "string", "index": "no", "copy_to": ["collector.fr"]},
                        "it": {"type": "string", "index": "no", "copy_to": ["collector.it"]},
                    }
                },

                'collector': {
                    "type": "object",
                    "properties": {
                        "en": {"type": "string", "analyzer": "stringanalyser",
                               "fields": {"raw": {"type": "string", "index_analyzer": "raw_stringanalyser",
                                                  "search_analyzer": "raw_stringanalyser"}}},
                        "de": {"type": "string", "analyzer": "stringanalyser",
                               "fields": {"raw": {"type": "string", "index_analyzer": "raw_stringanalyser",
                                                  "search_analyzer": "raw_stringanalyser"}}},
                        "fr": {"type": "string", "analyzer": "stringanalyser",
                               "fields": {"raw": {"type": "string", "index_analyzer": "raw_stringanalyser",
                                                  "search_analyzer": "raw_stringanalyser"}}},
                        "it": {"type": "string", "analyzer": "stringanalyser",
                               "fields": {"raw": {"type": "string", "index_analyzer": "raw_stringanalyser",
                                                  "search_analyzer": "raw_stringanalyser"}}},
                    }
                },

            }
        }
    }
    index_settings = {
        "analysis": {
            "analyzer": {
                "stringanalyser": {
                    "tokenizer": "standard",
                    "filter": ["lowercase", "asciifolding", "word_delimiter", "photonngram"],
                    "char_filter": ["punctuationgreedy", ]
                },
                "raw_stringanalyser": {
                    "tokenizer": "standard",
                    "filter": ["lowercase", "asciifolding", "word_delimiter"],
                    "char_filter": ["punctuationgreedy", ]
                },
            },
            "filter": {
                "photonngram": {
                    "type": "edgeNGram",
                    "min_gram": 2,
                    "max_gram": 15
                },
            },
            "char_filter": {
                "punctuationgreedy": {
                    "type": "pattern_replace",
                    "pattern": "[\.,]"
                },
            },
        }
    }

    es.indices.create(index, body={'mappings': mappings, 'settings': {'index': index_settings}})
    print('index created:', index)


if __name__ == "__main__":
    init_elasticsearch("photon")

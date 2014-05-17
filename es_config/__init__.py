import time
from elasticsearch import Elasticsearch
from elasticsearch.exceptions import NotFoundError


def init_elasticsearch(index, force=False):
    es = Elasticsearch()
    try:
        es.indices.delete(index)
        print('Deleted existing index:', index)
    except NotFoundError:
        pass
        
    mappings = {
        'place': {
            "dynamic": False,
            "_all": {"enabled": False},
            "_id": {"path": "id"},
            'properties': {
                'coordinate': {"type": "geo_point"},
                'osm_id': {"type": "long", "index": "not_analyzed"},

                'osm_key': {"type": "string", "index": "no"},
                'osm_type': {"type": "string", "index": "no"},
                'osm_value': {"type": "string", "index": "no"},

                'rank_search': {"type": "short", "index": "not_analyzed"},
                'street': {"type": "string", "index": "no", 
                           "copy_to": ["collector.en", "collector.de", "collector.fr", "collector.it"]},
                'housenumber': {"type": "string", "index": "not_analyzed"},
                'postcode': {"type": "string", "index": "no",
                             "copy_to": ["collector.en", "collector.de", "collector.fr", "collector.it"]},

                'name': {
                    "type": "object",
                    "properties": {
                        "default": {"type": "string",  "index": "no", "copy_to": ["collector.en", "collector.de", "collector.fr", "collector.it", "name.en", "name.de", "name.fr", "name.it"]},
                        "en": {"type": "string",  "index": "no",
                            "fields": {
                                "raw": {"type": "string", "index_analyzer": "raw_stringanalyser"},
                                "ngramed": {"type": "string", "index_analyzer": "stringanalyser"},
                            },
                            "copy_to": ["collector.en"]
                        },
                        "de": {"type": "string",  "index": "no",
                            "fields": {
                                "raw": {"type": "string", "index_analyzer": "raw_stringanalyser"},
                                "ngramed": {"type": "string", "index_analyzer": "stringanalyser"},
                            },
                            "copy_to": ["collector.de"]
                        },
                        "fr": {"type": "string",  "index": "no",
                            "fields": {
                                "raw": {"type": "string", "index_analyzer": "raw_stringanalyser"},
                                "ngramed": {"type": "string", "index_analyzer": "stringanalyser"},
                            },
                            "copy_to": ["collector.fr"]
                        },
                        "it": {"type": "string",  "index": "no",
                            "fields": {
                                "raw": {"type": "string", "index_analyzer": "raw_stringanalyser"},
                                "ngramed": {"type": "string", "index_analyzer": "stringanalyser"},
                            },
                            "copy_to": ["collector.it"]
                        },
                    }
                },

                'city': {
                    "type": "object",
                    "properties": {
                        "default": {"type": "string",  "index": "no",
                                    "copy_to": ["collector.en", "collector.de", "collector.fr", "collector.it"]},
                        "en": {"type": "string",  "index": "no", "copy_to": ["collector.en"]},
                        "de": {"type": "string",  "index": "no", "copy_to": ["collector.de"]},
                        "fr": {"type": "string",  "index": "no", "copy_to": ["collector.fr"]},
                        "it": {"type": "string",  "index": "no", "copy_to": ["collector.it"]},
                    }
                },

                'country': {
                    "type": "object",
                    "properties": {
                        "default": {"type": "string",  "index": "no",
                                    "copy_to": ["collector.en", "collector.de", "collector.fr", "collector.it"]},
                        "en": {"type": "string",  "index": "no", "copy_to": ["collector.en"]},
                        "de": {"type": "string",  "index": "no", "copy_to": ["collector.de"]},
                        "fr": {"type": "string",  "index": "no", "copy_to": ["collector.fr"]},
                        "it": {"type": "string",  "index": "no", "copy_to": ["collector.it"]},
                    }
                },

                'context': {
                    "type": "object",
                    "properties": {
                        "default": {"type": "string",  "index": "no",
                                    "copy_to": ["collector.en", "collector.de", "collector.fr", "collector.it"]},
                        "en": {"type": "string",  "index": "no", "copy_to": ["collector.en"]},
                        "de": {"type": "string",  "index": "no", "copy_to": ["collector.de"]},
                        "fr": {"type": "string",  "index": "no", "copy_to": ["collector.fr"]},
                        "it": {"type": "string",  "index": "no", "copy_to": ["collector.it"]},
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
    ngram_min = 2  # including word end delimiter.
    index_settings = {
        "analysis": {
            "analyzer": {
                "stringanalyser": {
                    "tokenizer": "standard",
                    "filter": ["word_delimiter", "lowercase", "asciifolding", "photonlength", "unique", "wordending", "photonngram"],
                    "char_filter": ["punctuationgreedy"],
                },
                "raw_stringanalyser": {
                    "tokenizer": "standard",
                    "filter": ["word_delimiter", "lowercase", "asciifolding", "photonlength", "unique", "wordending",],
                    "char_filter": ["punctuationgreedy"]
                },
                "search_stringanalyser": {
                    "tokenizer": "standard",
                    "filter": ["word_delimiter", "lowercase", "asciifolding", "photonlength", "unique", "wordendingautocomplete"],
                    "char_filter": ["punctuationgreedy"]
                },
            },
            "filter": {
                "photonngram": {
                    "type": "edgeNGram",
                    "min_gram": ngram_min,
                    "max_gram": 15
                },
                "photonlength": {
                    "type": "length",
                    "min": ngram_min
                },
                "wordending": {
                    "type": 'wordending',
                    "mode": "default"
                },
                "wordendingautocomplete": {
                    "type": 'wordending',
                    "mode": "autocomplete"
                },
            },
            "char_filter": {
                "punctuationgreedy": {
                    "type": "pattern_replace",
                    "pattern": "[\.,]"
                },
            },
            "similarity": {
                "photonsimilarity": {
                    "type": "BM25",
                }
            }
        }
    }

    es.indices.create(index, body={'mappings': mappings, 'settings': {'index': index_settings}})
    print('index created:', index)
    es.indices.put_alias(index, body={
	"actions" : [{
            "add" : {
                 "index" : index,
                 "alias" : "photon"
            }
        }]})



if __name__ == "__main__":
    init_elasticsearch("photon_" + time.strftime("%Y-%m-%d"))

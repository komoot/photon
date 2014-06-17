import json
import os

import simplejson
import elasticsearch
from flask import Flask, render_template, request, abort, Response


app = Flask(__name__)
DEBUG = os.environ.get('DEBUG', True)
PORT = os.environ.get('PHOTON_PORT', 5001)
HOST = os.environ.get('PHOTON_HOST', '0.0.0.0')
SUPPORTED_LANGUAGES = ['de', 'en', 'fr', 'it']

es = elasticsearch.Elasticsearch()


@app.route('/')
def index():
    return render_template('index.html')


def query_index(query, lang, lon, lat, match_all=True, limit=15):
    req_body = {
        "multi_match": {
            "query": query,
            "type": "cross_fields",
            "fields": [
              "name.default.raw^18", "name.default^2.5", "name.{lang}.raw^18", "name.{lang}^2.5", "name.alternatives.raw^14", "name.alternatives^1.5",
              "city.default.raw^8", "city.default^2", "city.{lang}.raw^8", "city.{lang}^2",
              "street.default.raw^8", "street.default^2", "street.{lang}.raw^8", "street.{lang}^2",
              "housenumber.raw^6", "housenumber",
              "postcode^5",
              "country.default.raw^3", "country.default", "country.{lang}.raw^3", "country.{lang}",
              "context.default.raw^3", "context.default", "context.{lang}.raw^3", "context.{lang}"
            ],
            "analyzer": "search",
            'minimum_should_match': '100%' if match_all else -1,
        }
    }

    # replace lang placeholder
    req_body['multi_match']['fields'] = map(lambda s: s.replace("{lang}", lang), req_body['multi_match']['fields'])

    req_body = {
        "function_score": {
            "score_mode": "sum",  # How functions score are mixed together
            "boost_mode": "sum",  # how total will be mixed to search score
            "query": req_body,
            "functions": [
                {
                    "script_score": {
                        "script": "1. + 50. * doc['importance'].value"
                    }
                }
            ],
        }
    }

    if False and lon is not None and lat is not None:
        req_body["function_score"]["functions"].append({
            "script_score": {
                "script": "dist = doc['coordinate'].distanceInKm(lat, lon); 1 / (0.5 - 0.5 * exp(-5*dist/maxDist))",
                "params": {
                    "lon": lon,
                    "lat": lat,
                    "maxDist": 100
                }
            }
        })

    # if housenumber is not null AND name.default is null, housenumber must
    # match the request. This will filter out the house from the requests
    # if the housenumber is not explicitly in the request
    req_body = {
        "filtered": {
            "query": req_body,
            "filter": {
                "or": {
                    "filters": [
                        {
                            "missing": {
                                "field": "housenumber"
                            }
                        },
                        {
                            "query": {
                                "match": {
                                    "housenumber": {
                                        "query": query,
                                        "analyzer": "standard"
                                    }
                                }
                            }
                        },
                        {
                            "exists": {
                                "field": "name.default.raw"
                            }
                        }
                    ]
                }
            }
        }
    }

    body = {"query": req_body, "size": limit}
    if DEBUG:
        print(json.dumps(body))
    return es.search(index="photon", body=body)


@app.route('/api/')
def api():
    lang = request.args.get('lang')
    if lang is None or lang not in SUPPORTED_LANGUAGES:
        lang = 'en'

    try:
        lon = float(request.args.get('lon'))
        lat = float(request.args.get('lat'))
    except (TypeError, ValueError):
        lon = lat = None

    try:
        limit = min(int(request.args.get('limit')), 50)
    except (TypeError, ValueError):
        limit = 15

    query = request.args.get('q')
    if not query:
        abort(400, "missing search term 'q': /?q=berlin")

    results = query_index(query, lang, lon, lat, limit=limit)

    if results['hits']['total'] < 1:
        # no result could be found, query index again and don't expect to match all search terms
        results = query_index(query, lang, lon, lat, match_all=False, limit=limit)

    debug = 'debug' in request.args
    data = to_geo_json(results['hits']['hits'], lang=lang, debug=debug)
    data = json.dumps(data, indent=4 if debug else None)
    return Response(data, mimetype='application/json')


def housenumber_first(lang):
    if lang in ['de', 'it']:
        return False

    return True


def to_geo_json(hits, lang='en', debug=False):
    features = []
    for hit in hits:
        source = hit['_source']

        properties = {}

        if 'osm_id' in source:
            properties['osm_id'] = int(source['osm_id'])

        for attr in ['osm_key', 'osm_value', 'postcode', 'housenumber']:
            if attr in source:
                properties[attr] = source[attr]

        # language specific mapping
        for attr in ['name', 'country', 'city', 'street']:
            obj = source.get(attr, {})
            value = obj.get(lang) or obj.get('default')
            if value:
                properties[attr] = value

        if not 'name' in properties and 'housenumber' in properties:
            housenumber = properties['housenumber'] or ''
            street = properties['street'] or ''

            if housenumber_first(lang):
                properties['name'] = housenumber + ' ' + street
            else:
                properties['name'] = street + ' ' + housenumber

        feature = {
            "type": "Feature",
            "geometry": {
                "type": "Point",
                "coordinates": [source['coordinate']['lon'], source['coordinate']['lat']]
            },
            "properties": properties
        }

        features.append(feature)

    return {
        "type": "FeatureCollection",
        "features": features
    }


@app.route('/search/')
def search():
    params = {
        "hl": 'true',
        "rows": 10
    }
    results = solr.search(request.args.get('q', '*:*'), **params)
    return simplejson.dumps({
        "docs": results.docs,
        "highlight": results.highlighting
    })


if __name__ == "__main__":
    app.run(debug=DEBUG, port=PORT, host=HOST)

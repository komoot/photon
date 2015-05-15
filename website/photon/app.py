import json
import os

import simplejson
import elasticsearch
from flask import Flask, render_template, request, abort, Response
from string import Template

app = Flask(__name__)
DEBUG = os.environ.get('DEBUG', False)
PORT = os.environ.get('PHOTON_PORT', 5001)
HOST = os.environ.get('PHOTON_HOST', '0.0.0.0')
API_URL = os.environ.get('API_URL', 'http://localhost:5001/api/?')
SUPPORTED_LANGUAGES = ['de', 'en', 'fr', 'it']
CENTER = [
    float(os.environ.get('PHOTON_MAP_LAT', 52.3879)),
    float(os.environ.get('PHOTON_MAP_LON', 13.0582))
]
TILELAYER = os.environ.get(
    'PHOTON_MAP_TILELAYER',
    '//www.komoot.de/tiles/{s}/{z}/{x}/{y}.png'
)
MAXZOOM = os.environ.get('PHOTON_MAP_MAXZOOM', 18)

es = elasticsearch.Elasticsearch()

with open('../../es/query.json', 'r') as f:
    query_template = Template(f.read())

with open('../../es/query_location_bias.json', 'r') as f:
    query_location_bias_template = Template(f.read())


@app.route('/')
def index():
    return render_template(
        'index.html',
        API_URL=API_URL,
        CENTER=CENTER,
        TILELAYER=TILELAYER,
        MAXZOOM=MAXZOOM
    )


def query_index(query, lang, lon, lat, match_all=True, limit=15):
    params = dict(lang=lang, query=query, should_match="100%" if match_all else "-1", lon=lon, lat=lat)

    if lon is not None and lat is not None:
        req_body = query_location_bias_template.substitute(**params)
    else:
        req_body = query_template.substitute(**params)

    if DEBUG:
        print(req_body)

    body = {"query": json.loads(req_body), "size": limit}
    return es.search(index="photon", body=body)


#@app.route('/api/')
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

        for attr in ['osm_key', 'osm_value', 'postcode', 'housenumber', 'osm_type']:
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

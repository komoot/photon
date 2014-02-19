import json
import os

import simplejson
import pysolr
from flask import Flask, render_template, request, abort


app = Flask(__name__)
DEBUG = os.environ.get('DEBUG', False)
PORT = os.environ.get('PHOTON_PORT', 5001)
SUPPORTED_LANGUAGES = ['de', 'en', 'fr', 'it']

solr = pysolr.Solr(os.environ.get("SOLR_ENDPOINT", 'http://localhost:8983/solr/'), timeout=10)


@app.route('/')
def index():
    return render_template('index.html')


@app.route('/api/')
def api():
    params = {}

    lang = request.args.get('lang')
    if lang is None or lang not in SUPPORTED_LANGUAGES:
        lang = 'en'

    try:
        lon = float(request.args.get('lon'))
        lat = float(request.args.get('lat'))
        params['qt'] = '{lang}_loc'.format(lang=lang)
        params['pt'] = '{lat},{lon}'.format(lon=lon, lat=lat)
    except (TypeError, ValueError):
        params['qt'] = '{lang}'.format(lang=lang)

    try:
        params['rows'] = int(request.args.get('limit'))
    except (TypeError, ValueError):
        params['rows'] = 15

    query = request.args.get('q')
    if not query:
        abort(400, "missing search term 'q': /?q=berlin")

    results = solr.search(query, **params)
    return json.dumps(to_geo_json(results.docs))


def to_geo_json(docs, lang='en'):
    features = []

    for doc in docs:
        properties = {}
        for attr in ['name', 'osm_id', 'osm_key', 'osm_value', 'street', 'city', 'postcode', 'country']:
            if attr in doc:
                properties[attr] = doc[attr]

        coordinates = [float(el) for el in doc['coordinate'].split(',')]
        coordinates.reverse()

        feature = {
            "type": "Feature",
            "geometry": {
                "type": "Point",
                "coordinates": coordinates
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
    app.run(debug=DEBUG, port=PORT)

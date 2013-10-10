import os
import simplejson

import pysolr
from flask import Flask, render_template, request


app = Flask(__name__)
DEBUG = os.environ.get('DEBUG', False)
PORT = os.environ.get('PHOTON_PORT', 5001)
solr = pysolr.Solr(os.environ.get("SOLR_ENDPOINT", 'http://localhost:8983/solr/'), timeout=10)

@app.route('/')
def index():
    return render_template('index.html')


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
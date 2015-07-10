import os

from flask import Flask, render_template, request, abort, Response

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


@app.route('/')
def index():
    return render_template(
        'index.html',
        API_URL=API_URL,
        CENTER=CENTER,
        TILELAYER=TILELAYER,
        MAXZOOM=MAXZOOM
    )


if __name__ == "__main__":
    app.run(debug=DEBUG, port=PORT, host=HOST)

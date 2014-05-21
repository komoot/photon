import requests


POTSDAM = [52.3879, 13.0582]
BERLIN = [52.519854, 13.438596]
MUNICH = [43.731245, 7.419744]
AUCKLAND = [-36.853467, 174.765551]
CONFIG = {
    'PHOTON_URL': "http://localhost:5001/api/"
}


class SearchException(Exception):
    """ custom exception for error reporting. """

    def __init__(self, **kwargs):
        super().__init__()
        self.results = kwargs.get("results", {})


def search(**params):
    r = requests.get(CONFIG['PHOTON_URL'], params=params)
    if not r.status_code == 200:
        raise SearchException("Non 200 response")
    return r.json()


def assert_search(query, expected, limit=1,
                  comment=None, lang=None, center=None):
    params = {"q": query, "limit": limit}
    if lang:
        params['lang'] = lang
    if center:
        params['lat'] = center[0]
        params['lon'] = center[1]
    results = search(**params)

    def assert_expected(expected):
        found = False
        for r in results['features']:
            found = True
            for key, value in expected.items():
                if not key in r['properties'] or not r['properties'][key] == value:
                    found = False
        if not found:
            raise SearchException(results=str(results))

    if not isinstance(expected, list):
        expected = [expected]
    for s in expected:
        assert_expected(s)

import requests


POTSDAM = [52.3879, 13.0582]
BERLIN = [52.519854, 13.438596]
MUNICH = [43.731245, 7.419744]
AUCKLAND = [-36.853467, 174.765551]
CONFIG = {
    'PHOTON_URL': "http://localhost:5001/api/"
}


class HttpSearchException(Exception):

    def __init__(self, **kwargs):
        super().__init__()
        self.error = kwargs.get("error", {})

    def __str__(self):
        return self.error


class SearchException(Exception):
    """ custom exception for error reporting. """

    def __init__(self, **kwargs):
        super().__init__()
        self.results = kwargs.get("results", {})
        self.query = kwargs.get('query', '')
        self.expected = kwargs.get('expected', {})

    def __str__(self):
        lines = [
            '',
            'Search failed',
            "# Search was: {}".format(self.query),
        ]
        expected = '# Expected was: '
        expected += " | ".join("{}: {}".format(k, v) for k, v in self.expected.items())
        lines.append(expected)
        lines.append('# Results were:')
        keys = ['name', 'osm_key', 'osm_value', 'osm_id', 'housenumber',
                'street', 'postcode', 'city', 'country']
        results = [f['properties'] for f in self.results['features']]
        lines.extend(dicts_to_table(results, keys=keys))
        lines.append('')
        return "\n".join(lines)


def search(**params):
    r = requests.get(CONFIG['PHOTON_URL'], params=params)
    print(CONFIG['PHOTON_URL'], params)
    if not r.status_code == 200:
        raise HttpSearchException(error="Non 200 response")
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
                if not str(r['properties'].get(key)) == str(value):
                    found = False
            if found:
                break
        if not found:
            raise SearchException(
                results=results,
                query=query,
                expected=expected
            )

    if not isinstance(expected, list):
        expected = [expected]
    for s in expected:
        assert_expected(s)


def dicts_to_table(dicts, keys=None):
    if not dicts:
        return []
    if keys is None:
        keys = dicts[0].keys()
    cols = []
    for i, key in enumerate(keys):
        cols.append(len(key))
    for d in dicts:
        for i, key in enumerate(keys):
            l = len(str(d.get(key, '')))
            if l > cols[i]:
                cols[i] = l
    out = []

    def fill(l, to, char=" "):
        l = str(l)
        return "{}{}".format(
            l,
            char * (to - len(l) if len(l) < to else 0)
        )

    def create_row(values, char=" "):
        row = []
        for i, v in enumerate(values):
            row.append(fill(v, cols[i], char))
        return " | ".join(row)

    out.append(create_row(keys))
    out.append(create_row(['' for k in keys], char="-"))
    for d in dicts:
        out.append(create_row([d.get(k, '—') for k in keys]))
    return out

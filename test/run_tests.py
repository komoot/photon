import unittest
import os
import requests

from dataset import Dataset

queries_folder = 'iledefrance'
queries_folder_path = os.path.join(
    os.path.dirname(os.path.realpath(__file__)),
    queries_folder
)

URL = "http://localhost:5001/api/"


def assertMatch(search, expected, limit=1, comment=None, lang=None,
                center=None):
    params = {"q": search, "limit": limit}
    if lang:
        params['lang'] = lang
    if center:
        params['lat'], params['lon'] = center.split(',')

    results = requests.get(URL, params=params).json()

    assert len(results['features']) > 0, "The service returned 0 results"

    data = results['features'][0]['properties']

    if expected.get('expected_city'):
        assert 'city' in data, 'There is no city property in the result'
        assert data['city'] == expected['expected_city'], 'Returned city is not the expected city'

    if expected.get('expected_country'):
        assert data['country'] == expected['expected_country'], 'Returned country is not the expected country'

    if expected.get('expected_street'):
        assert data['street'] == expected['expected_street'], 'Returned street is not the expected street'

    if expected.get('expected_postcode'):
        assert data['postcode'] == expected['expected_postcode'], 'Returned postcode is not the expected postcode'

    # def assertExpected(expected):
    #     found = False
    #     for r in results['features']:
    #         found = True
    #         for key, value in expected.items():
    #             if not key in r['properties'] or not r['properties'][key] == value:
    #                 found = False
    #     if not found:
    #         if comment:
    #             msg = "{}\nResults were:\n{}".format(comment, results)
    #         else:
    #             msg = "{expected} not found in {results}".format(results=results, expected=expected)
    #         self.fail(msg)


def test_datasets_generator():

    def check_one(data):
        print('Testing query: %s' % data['tried_query'])
        assertMatch(
            data['tried_query'],
            data,
            center=data.get('tried_location', None)
        )

    for data in Dataset.import_from_path(queries_folder_path):
        yield check_one, data


if __name__ == '__main__':
    unittest.main(verbosity=2)

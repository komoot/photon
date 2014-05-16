import unittest
import sys
import os
import csv
import requests

from dataset import Dataset

URL = "http://photon.komoot.de/api/"

class CSVDatasetTests(unittest.TestCase):
    QUERIES = 'queries'

    def setUp(self):
        self.QUERIES_PATH = os.path.join(os.path.dirname(os.path.realpath(__file__)), self.QUERIES)


    def tests_from_datasets(self):
        for testset in Dataset.import_from_path(self.QUERIES_PATH):
            print('Testing query: %s' % testset['tried_query'])
            try:
                self.assertMatch(testset['tried_query'], testset, center=testset.get('tried_location', None))
            except AssertionError as e:
                print("### FAIL: %s" % e)


    def assertMatch(self, search, expected, limit=1, comment=None, lang=None, center=None):
        params = {"q": search, "limit": limit}
        if lang:
            params['lang'] = lang
        if center:
            params['lat'], params['lon'] = center.split(',')

        results = requests.get(URL, params=params).json()
        
        self.assertTrue(len(results['features']) > 0, "The service returned 0 results")

        data = results['features'][0]['properties']

        def assert_property(expected_property_name):
            expected_value = expected.get(expected_property_name) 
            # This shortcut is based on the "happy coincidence" that photon's result properties match the names of the
            # expected properties in the CSV.  
            result_property_name = expected_property_name[len('expected_'):]
            if expected_value:
                if expected_value == 'NULL':
                    self.assertFalse(data.get(result_property_name), 'There is a %s property in the result, but it should\'t' % result_property_name)
                else:
                    self.assertTrue(result_property_name in data, 'There is no %s property in the result' % result_property_name)
                    self.assertEqual(data[result_property_name], expected[expected_property_name], 'Returned %s is not the expected %s' % (result_property_name, result_property_name))

        assert_property('expected_city')
        assert_property('expected_country')
        assert_property('expected_street')
        assert_property('expected_postcode')
        assert_property('expected_housenumber')


if __name__ == '__main__':
    # TODO: Clean arguments parsing:
    # - an argument for the queries folder
    # - an argument to define the geocoder implementation (photon/nominatim/...)
    # - an argument to define the URL of the service
    if len(sys.argv) > 1:
        CSVDatasetTests.QUERIES = sys.argv.pop()
    else:
        CSVDatasetTests.QUERIES = 'queries'
    unittest.main(verbosity=2)
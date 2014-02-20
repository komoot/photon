import unittest

import requests


class PhotonImplementationTest(unittest.TestCase):

    URL = "http://photon.komoot.de/api/"

    def assertMatch(self, search, expected, limit=1, comment=None, lang=None, center=None):
        params = {"q": search, "limit": limit}
        if lang:
            params['lang'] = lang
        if center:
            params['lat'] = center[0]
            params['lon'] = center[1]
        results = requests.get(self.URL, params=params).json()

        def assertExpected(expected):
            found = False
            for r in results['features']:
                found = True
                for key, value in expected.items():
                    if not key in r['properties'] or not r['properties'][key] == value:
                        found = False
            if not found:
                if comment:
                    msg = "{}\nResults were:\n{}".format(comment, results)
                else:
                    msg = "{expected} not found in {results}".format(results=results, expected=expected)
                self.fail(msg)

        if not isinstance(expected, list):
            expected = [expected]
        for s in expected:
            assertExpected(s)


class MainCitiesTest(PhotonImplementationTest):

    def test_berlin(self):
        self.assertMatch("berlin", {'name': 'Berlin'})

    def test_paris(self):
        self.assertMatch("paris", {'name': 'Paris'})

    def test_new_york(self):
        self.assertMatch("new york", {'name': 'New York'})


POTSDAM = [52.3879, 13.0582]
BERLIN = [52.519854, 13.438596]


class LocationBiasTest(PhotonImplementationTest):

    def test_ber_from_potsdam(self):
        self.assertMatch("ber", {'name': 'Berlin'}, center=POTSDAM, limit=3, comment="I can find Berlin from Potsdam typing 'ber'")

    def test_friedrichstrasse_from_potsdam(self):
        self.assertMatch("Friedrichstraße", {'name': 'Friedrichstraße', 'city': 'Werder (Havel)'}, center=POTSDAM, limit=1, comment="'Friedrichstraße' gives me Werder's street when I'm in Potsdam")

    def test_friedrichstrasse_from_berlin(self):
        self.assertMatch("Friedrichstraße", {'name': 'Friedrichstraße', 'city': 'Berlin'}, center=BERLIN, limit=1, comment="'Friedrichstraße' gives me Berlin's street when I'm in Berlin")


class SmallPlacesTest(PhotonImplementationTest):

    def test_port_aux_cerises(self):
        self.assertMatch("port aux cerises", {'osm_id': 833452505}, limit=2, comment="I can find 'Port aux Cerises' harbour")


class LangTest(PhotonImplementationTest):

    def test_munchen_without_lang(self):
        self.assertMatch("munchen", {'name': 'München'})

    def test_munchen_with_lang_de(self):
        self.assertMatch("munchen", {'name': 'München'})


if __name__ == '__main__':
    unittest.main(verbosity=2)

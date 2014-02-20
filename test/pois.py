import unittest

from test.base import PhotonImplementationTest


class Pois(PhotonImplementationTest):
    def test_teufelsberg(self):
        self.assertMatch("teufelsberg ber", {'osm_id': 156850034})


if __name__ == '__main__':
    unittest.main(verbosity=2)

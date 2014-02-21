import unittest

from test.base import PhotonImplementationTest


class Pois(PhotonImplementationTest):

    def test_teufelsberg(self):
        self.assertMatch("teufelsberg ber", {'osm_id': 156850034})

    def test_c_tout_bio_cergy(self):
        self.assertMatch("c tout bio cergy", {'osm_id': 2406109852}, comment="I can find 'C'Tout Bio' shop in Cergy")

    def test_canal_bio_paris(self):
        self.assertMatch("canal bio paris", {'osm_id': 627857438}, comment="I can find 'Canal Bio' shop in Paris")


if __name__ == '__main__':
    unittest.main(verbosity=2)

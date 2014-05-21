import pytest

from ..base import assert_search


@pytest.mark.germany
def test_teufelsberg():
    assert_search("teufelsberg ber", {'osm_id': 156850034})


@pytest.mark.france
@pytest.mark.iledefrance
def test_c_tout_bio_cergy():
    assert_search("c tout bio cergy", {'osm_id': 2406109852})


@pytest.mark.france
@pytest.mark.iledefrance
def test_canal_bio_paris():
    assert_search("canal bio paris", {'osm_id': 627857438})

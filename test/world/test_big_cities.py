import pytest

from ..base import assert_search, BERLIN, POTSDAM, AUCKLAND, MUNICH


@pytest.mark.germany
def test_berlin():
    assert_search("berlin", {'name': 'Berlin'})


@pytest.mark.france
@pytest.mark.iledefrance
def test_paris():
    assert_search("paris", {'name': 'Paris'})


def test_new_york():
    assert_search("new york", {'name': 'New York'})


def test_vienna():
    assert_search("vienna", {'name': 'Vienna', 'country': 'Austria'})


@pytest.mark.france
def test_toulouse():
    assert_search("toulouse", {'name': 'Toulouse', 'country': 'France'})


def test_milan():
    assert_search("milan", {'name': 'Milan', 'country': 'Italy'})
    assert_search("milano", {'name': 'Milan', 'country': 'Italy'})


@pytest.mark.germany
def test_ber_from_potsdam():
    assert_search("ber", {'name': 'Berlin'}, center=POTSDAM, limit=3,
                  comment="I can find Berlin from Potsdam typing 'ber'")


@pytest.mark.germany
def test_friedrichstrasse_from_potsdam():
    assert_search("Friedrichstraße", {'name': 'Friedrichstraße', 'city': 'Werder (Havel)'}, center=POTSDAM,
                  limit=1, comment="'Friedrichstraße' gives me Werder's street when I'm in Potsdam")


@pytest.mark.germany
def test_friedrichstrasse_from_berlin():
    assert_search("Friedrichstraße", {'name': 'Friedrichstraße', 'city': 'Berlin'}, center=BERLIN, limit=1,
                     comment="'Friedrichstraße' gives me Berlin's street when I'm in Berlin")


@pytest.mark.france
@pytest.mark.iledefrance
def test_paris_from_new_zeland():
    assert_search("paris", {"name": "Paris"}, center=AUCKLAND, limit=1, comment="'Paris' from Auclkand still gives Paris")


@pytest.mark.germany
def test_munchen_without_lang():
    assert_search("munchen", {'name': 'München'})


@pytest.mark.germany
def test_munchen_with_lang_de():
    assert_search("munchen", {'name': 'München'}, lang="de")


@pytest.mark.germany
def test_munchen_with_lang_fr():
    assert_search("Munich", {'name': 'Munich'}, lang="fr")


@pytest.mark.germany
def test_munchen_with_lang_it():
    assert_search("Monaco", {'osm_id': 36990}, lang="it", limit=2,
                     comment="'Monaco' should hit Munich in the two first results when lang is italian")


@pytest.mark.germany
def test_munchen_with_lang_it_and_geolocation_bias():
    assert_search("Monaco", {'osm_id': 36990}, lang="it", limit=1, center=MUNICH,
                  comment="'Monaco' should hit Munich as first result when lang is italian and center is close to Munich")

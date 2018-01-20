from geocoder_tester.base import search, assert_search


def test_housenumbers_are_missing():
    results = search(q='rue bergère paris')
    for r in results['features']:
        assert (not r['properties'].get('housenumber')
                or r['properties'].get('name'))


def test_25_rue_bergere():
    assert_search('25 rue bergère paris', {'housenumber': 25, 'street': 'Rue Bergère'})


def test_22_rue_vicq_d():
    assert_search("22 rue vicq d'", {'housenumber': '22', "osm_value": "house", "street": "Rue Vicq d'Azir", "city": "Paris"})


def test_22_rue_vicq_d_a():
    assert_search("22 rue vicq d'a", {'housenumber': '22', "osm_value": "house", "street": "Rue Vicq d'Azir", "city": "Paris"})


def test_22_rue_vicq_d_azir():
    assert_search("22 rue vicq d'azir", {'housenumber': '22', "osm_value": "house", "street": "Rue Vicq d'Azir", "city": "Paris"})


def test_8_des_pyrenees():
    assert_search("8 rue des pyrénées paris", {'housenumber': '8', "osm_value": "house", "street": "Rue des Pyrénées", "city": "Paris"})


def test_boulevard_jourdan_reverted():
    assert_search("Boulevard Jourdan 95", {'osm_id': 1773529891})

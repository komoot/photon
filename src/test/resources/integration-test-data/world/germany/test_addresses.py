from ...base import assert_search, BERLIN


def test_berlin_1():
    assert_search("dircksenstr 51", {'osm_id': 1552576044})


def test_berlin_2():
    assert_search("Rosa-Luxemburg-Straße 22", {'osm_id': 736787797}, center=BERLIN)


def test_au():
    assert_search("Jaghausen 4", {'osm_id': 135405796})


def test_stockholm():
    assert_search("Källargränd 3", {'osm_id': 1815123244})


def test_birmingham():
    assert_search("9 Ivor Road Birmingham", {'osm_id': 144738133})

import csv
import yaml

import pytest

from .base import SearchException, assert_search, CONFIG


def pytest_collect_file(parent, path):
    if not path.basename.startswith("test"):
        return None
    f = None
    ext = path.ext
    if ext == ".csv":
        f = CSVFile(path, parent)
    if ext == ".yml":
        f = YamlFile(path, parent)
    return f


def pytest_itemcollected(item):
    dirs = item.session.fspath.bestrelpath(item.fspath.dirpath()).split('/')
    for d in dirs:
        if d != ".":
            item.add_marker(d)


def pytest_addoption(parser):
    parser.addoption(
        '--photon-url',
        dest="photon_url",
        default=CONFIG['PHOTON_URL'],
        help="The URL to use for running Photon tests."
    )


def pytest_configure(config):
    CONFIG['PHOTON_URL'] = config.getoption('--photon-url')


class CSVFile(pytest.File):

    def collect(self):
        with self.fspath.open() as f:
            dialect = csv.Sniffer().sniff(f.read(1024))
            f.seek(0)
            reader = csv.DictReader(f, dialect=dialect)
            for row in reader:
                yield CSVItem(row, self)


class YamlFile(pytest.File):

    def collect(self):
        raw = yaml.safe_load(self.fspath.open())
        for name, spec in raw.items():
            yield YamlItem(name, self, spec)


class BaseFlatItem(pytest.Item):

    def runtest(self):
        kwargs = {
            'query': self.query,
            'expected': self.expected,
            'lang': self.lang,
            'comment': self.comment
        }
        if self.lat and self.lon:
            kwargs['center'] = [self.lat, self.lon]
        if self.limit:
            kwargs['limit'] = self.limit
        assert_search(**kwargs)

    def repr_failure(self, excinfo):
        """ called when self.runtest() raises an exception. """
        return str(excinfo.value)

    def reportstring(self):
        s = "Search: {}".format(self.query)
        if self.comment:
            s = "{} ({})".format(s, self.comment)
        return s

    def reportinfo(self):
        return self.fspath, 0, self.reportstring()


class CSVItem(BaseFlatItem):

    def __init__(self, row, parent):
        super(CSVItem, self).__init__(row.get('comment', ''), parent)
        self.query = row.get('query', '')
        self.expected = {}
        self.lat = row.get('lat')
        self.lon = row.get('lon')
        self.lang = row.get('lang')
        self.limit = row.get('limit')
        self.comment = row.get('comment')
        for key, value in row.items():
            if key.startswith('expected_') and value:
                self.expected[key[9:]] = value


class YamlItem(BaseFlatItem):
    def __init__(self, name, parent, spec):
        super(YamlItem, self).__init__(name, parent)
        self.query = name
        self.expected = spec['expected']
        self.lat = spec.get('lat')
        self.lon = spec.get('lon')
        self.lang = spec.get('lang')
        self.limit = spec.get('limit')
        self.comment = spec.get('comment')

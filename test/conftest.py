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
            reader = csv.DictReader(f)
            for row in reader:
                yield CSVItem(row, self)


class YamlFile(pytest.File):

    def collect(self):
        raw = yaml.safe_load(self.fspath.open())
        for name, spec in raw.items():
            yield YamlItem(name, self, spec)


class BaseFlatItem(pytest.Item):

    def runtest(self):
        assert_search(
            query=self.query,
            expected=self.expected
        )

    def repr_failure(self, excinfo):
        """ called when self.runtest() raises an exception. """
        return str(excinfo.value)

    def reportinfo(self):
        return self.fspath, 0, "Search: {}".format(self.query)


class CSVItem(BaseFlatItem):

    def __init__(self, row, parent):
        super(CSVItem, self).__init__(row.get('comment', ''), parent)
        self.query = row.get('query', '')
        self.expected = {}
        for key, value in row.items():
            if key.startswith('expected_') and value is not None:
                self.expected[key[9:]] = value


class YamlItem(BaseFlatItem):
    def __init__(self, name, parent, spec):
        super(YamlItem, self).__init__(name, parent)
        self.query = name
        self.expected = spec['expected']

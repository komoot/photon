import csv

import pytest

from .base import SearchException, assert_search, CONFIG


def pytest_collect_file(parent, path):
    if not path.basename.startswith("test"):
        return None
    f = None
    ext = path.ext
    if ext == ".csv":
        f = CSVFile(path, parent)
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


class CSVItem(pytest.Item):
    def __init__(self, row, parent):
        super(CSVItem, self).__init__(row.get('comment', ''), parent)
        self.query = row.get('query', '')
        self.expected = {}
        for key, value in row.items():
            if key.startswith('expected_') and value is not None:
                self.expected[key[9:]] = value

    def runtest(self):
        assert_search(
            search=self.query,
            expected=self.expected
        )

    def repr_failure(self, excinfo):
        """ called when self.runtest() raises an exception. """
        if isinstance(excinfo.value, SearchException):
            return "\n".join([
                "Search failed",
                "   search was: {}".format(self.query),
                "   results were: {}".format(excinfo.value.results),
            ])

    def reportinfo(self):
        return self.fspath, 0, "Search: {}".format(self.query)

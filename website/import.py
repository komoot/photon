"""
This is just a raw importer for the sprint.
"""

import os
import psycopg2
import psycopg2.extras
import simplejson

from elasticsearch import Elasticsearch
from elasticsearch.helpers import bulk_index

es = Elasticsearch()


class NominatimExporter(object):

    ITER_BY = 100

    def __init__(self, credentials, itersize=1000, limit=None, **kwargs):
        self.credentials = credentials
        print('***************** Export init ***************')
        self.conn = self.create_connexion()
        print('Connected')
        self.cur = self.conn.cursor("nominatim", cursor_factory=psycopg2.extras.DictCursor)
        print('Cursor created')
        self.cur.itersize = itersize
        self.limit = limit
        self.kwargs = kwargs

    def create_connexion(self):
        return psycopg2.connect(**self.credentials)

    def __enter__(self):
        sql = """SELECT osm_type,osm_id,class as osm_key,type as osm_value,admin_level,rank_search,rank_address,
            place_id,parent_place_id,calculated_country_code as country_code, postcode, housenumber,
            (extratags->'ref') as ref, street,
            ST_X(ST_Centroid(geometry)) as lon,
            ST_Y(ST_Centroid(geometry)) as lat,
            name->'name' as name,
            name->'name:de' as name_de,
            name->'name:fr' as name_fr,
            name->'name:en' as name_en,
            name->'short_name' as short_name,
            name->'official_name' as official_name, name->'alt_name' as alt_name,
            (extratags->'place') as extra_place
            FROM placex
            ORDER BY geometry
            """
        self.cur.execute(sql)
        print('Query executed with itersize', self.cur.itersize)
        return self

    def get_name_clause(self):
        return "name->'name' as name"

    def add_parents(self, row):
        if not "context_name" in row:
            row['context_name'] = []
        self.add_parent(row, row)

    def add_parent(self, child, row):
        if child['parent_place_id']:
            sql = """SELECT parent_place_id, type as osm_value, class as osm_key, {name}, admin_level FROM placex WHERE place_id={parent_place_id}"""
            sql = sql.format(**{
                'parent_place_id': child['parent_place_id'],
                'name': self.get_name_clause()
            })
            cur = self.conn.cursor(str(child['parent_place_id']), cursor_factory=psycopg2.extras.DictCursor)
            cur.execute(sql)
            parent = cur.fetchone()
            cur.close()
            self.add_parent_data(parent, row)
            self.add_parent(parent, row)

    def add_parent_data(self, parent, row):
        if not parent:
            return
        parent = dict(parent)
        name = parent['name']
        if name and not name in row['context_name']:
            row["context_name"].append(name)
        if (parent.get('osm_key') == "boundary"
           and parent.get('osm_value') == "administrative"
           and parent.get('admin_level') == 8):
            row['city'] = {
                'default': name
            }

    def __exit__(self, *args):
        self.cur.close()
        self.conn.close()

    def __iter__(self):
        return self.cur.__iter__()

    def to_json(self, raw):
        row = dict(raw)
        self.add_parents(row)
        row['coordinate'] = {
            'lon': raw['lon'],
            'lat': raw['lat']
        }
        row['name'] = {
            'default': raw['name'],
            'de': raw['name_de'],
            'fr': raw['name_fr'],
            'en': raw['name_en'],
        }
        row['id'] = '_'.join([raw['osm_type'], str(raw['osm_id'])])
        row['importance'] = 0.75 - raw['rank_search'] / 40
        row['context'] = {
            'default': row['context_name']
        }
        return row


class BaseConsumer(object):

    def __iter__(self):
        credentials = {
            'dbname': os.environ.get('DBNAME', 'nominatim')
        }
        with NominatimExporter(credentials) as exporter:
            for raw in exporter:
                yield exporter.to_json(raw)


class ESImporter(BaseConsumer):

    INDEX_CHUNK_SIZE = 10000
    ES_INDEX = "photon"

    def __call__(self):
        count = 0
        data = []
        print('Starting with ES index', self.ES_INDEX)
        for row in self:
            data.append(row)
            count += 1
            if count >= self.INDEX_CHUNK_SIZE:
                self.index(data)
                count = 0
                data = []
        if data:
            self.index(data)
        print('Done!')

    def format(self, row):
        return row

    def suggest_payload(self, row):
        return {
            "latlon": row['latlon'],
            'type': row['type'],
            'source': row['source'],
            'class': row['class'],
        }

    def exclude_row(self, row):
        return False

    def index(self, data):
        print('Start indexing batch of', len(data))
        bulk_index(es, data, index=self.ES_INDEX, doc_type='place', refresh=True)
        print('End indexing of current batch')

    def join(self, l, sep=", "):
        return sep.join([str(s) for s in l if str(s).strip()])

    def set_id(self, row, *args):
        row['_id'] = self.join(args, "_")


class JSONBatchDump(BaseConsumer):
    """Format for ES batch import."""

    MAX_ROWS = 100000

    def __call__(self):
        data = []
        count = 0
        index = 0
        print('Starting export')

        def do_write(data, index):
            with open('dump{}.eson'.format(index), mode='w', encoding='utf-8') as f:
                f.write('\n'.join(data))

        for row in self:
            data.append('{"index": {}}')
            data.append(simplejson.dumps(row))
            count += 1
            if count % 10000 == 0:
                print('Processed:', count, index)
            if count >= self.MAX_ROWS:
                do_write(data, index)
                data = []
                count = 0
                index += 1
        do_write(data)
        print('Done!')


if __name__ == "__main__":
    importer = JSONBatchDump()
    importer()

import unittest
import sys
import os
import csv

from base import PhotonImplementationTest

queries_folder = sys.argv[1] if len(sys.argv) > 1 else 'queries'
queries_folder_path = os.path.join(os.path.dirname(os.path.realpath(__file__)), queries_folder)

class CSVSourceTests(PhotonImplementationTest):

    def test_from_csv(self):
        for filepath in os.listdir(queries_folder_path):
            filename, filext = os.path.splitext(filepath)
            if os.path.isfile(os.path.join(queries_folder, filepath)) and filext == '.csv':
                # This is a queries file
                print('* Running test suite from file %s' % filename)
                with open(os.path.join(queries_folder_path, filepath)) as csv_file:
                    reader = csv.DictReader(csv_file)
                    for query in reader:
                        print(query)


if __name__ == '__main__':
    unittest.main(verbosity=2)
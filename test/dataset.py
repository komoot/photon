import os
import sys
import csv

class Dataset:

  # Imports a CSV datasets from a given directory / file and generates
  # an array of dictionaries (datasets maped to attributes).
  def import_from_path(path):
    sets = []
    if os.path.isdir(path):
      csv_paths = []
      for(dirpath, dirnames, filenames) in os.walk(path):
        for filename in filenames:
          csv_paths.append(dirpath + "/" + filename)
        break
      for csv_path in csv_paths:
        sets.extend(Dataset.import_csv(csv_path))
    elif os.path.isfile(path):
      sets.extend(Dataset.import_csv(path))

    return sets

  def import_csv(path):
    sets = []
    with open(path, "r") as source:
      rows = csv.reader(source, delimiter = ";")
      attributes = []
      for row in rows:
        if len(attributes) == 0:
          attributes = row
        else:
          data = dict()
          for i, attribute in enumerate(attributes):
            data[attribute] = row[i]
          sets.append(data)

    return sets

if __name__ == "__main__":
  sets = Dataset.import_from_path(sys.argv[1])
  print(sets)
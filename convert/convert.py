# script to convert solr xml to json objects suitable for elastics search

import json
import xml.etree.cElementTree as et


input_xml = '../../../data/komoot_solr.xml'

I18N_FIELDS = "name", "city", "country", "places"
int_fields = []
int_fields += I18N_FIELDS

LANGUAGES = ["de", "en"]

for field in I18N_FIELDS:
    int_fields += [field + "_" + lang for lang in LANGUAGES]


def nested_keys(field_name):
    keys = field_name.split("_")

    if len(keys) < 2:
        keys.append('default')

    if keys[0] == 'places':
        keys[0] = 'context'

    return keys


ONLY_GERMANY = True

INSERT_LINE = """{"index":{}}"""

xml_tree = et.iterparse(input_xml)
output_xml = input_xml.replace('.xml', '.json')

count = 0
with open(output_xml, 'w') as f:
    doc = {}

    for event, elem in xml_tree:
        if elem.tag == 'field':
            attr = elem.attrib['name']

            if attr == "name" and "name" in doc:
                # name already exists, add it to context (default)
                if not "context" in doc:
                    doc["context"] = {}

                if not "default" in doc["context"]:
                    doc["context"]["default"] = ""
                    
                if elem.text:
                    doc["context"]["default"] += ", " + elem.text
            elif attr in int_fields:
                key, sub_key = nested_keys(attr)
                if key not in doc:
                    doc[key] = {}
                doc[key][sub_key] = elem.text
            elif attr == "ranking":
                doc["importance"] = float(elem.text) / 40
            elif attr in ["osm_id", "category", "id"]:
                doc[attr] = int(elem.text)
            elif attr == "coordinate":
                ll = elem.text.split(",")
                doc[attr] = {
                    "lat": float(ll[0]),
                    "lon": float(ll[1])
                }
            elif attr == "street":
                doc[attr] = {"default": elem.text}
            else:
                doc[attr] = elem.text

        elif elem.tag == 'doc':
            # document is finished, dump it to json
            count += 1
            if not count % 10000:
                print("progress: {:,}".format(count))

            # check extent
            if "coordinate_sw" in doc and "coordinate_ne":
                sw = map(float, doc["coordinate_sw"].split(","))
                ne = map(float, doc["coordinate_ne"].split(","))

                del doc["coordinate_sw"]
                del doc["coordinate_ne"]

                doc["extent"] = {
                    "type": "envelope",
                    "coordinates": [[sw[0], ne[1]], [ne[0], sw[1]]]
                }

            f.write(INSERT_LINE + "\n")
            json.dump(doc, f)
            f.write("\n")

            doc = {}

        elem.clear()

    f.write("\n")

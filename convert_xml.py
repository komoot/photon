# script to convert solr xml to check out other approaches
import json

import xml.etree.cElementTree as et

#input_xml = '/home/photon/data/solr-131012.xml'
input_xml = 'sample_data/iceland.solr.xml'

I18N_FIELDS = "name", "city", "country", "places"
int_fields = []
int_fields += I18N_FIELDS

LANGUAGES = ["de", "en", "fr", "it", 'es']
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

INSERT_LINE = """{ "index" : { "_index" : "photon", "_type" : "place"} }\n"""

xml_tree = et.iterparse(input_xml)
output_xml = input_xml.replace('.xml', '.json')

count = 0
with open(output_xml, 'w') as f:
    doc = {}

    for event, elem in xml_tree:
        if elem.tag == 'field':
            attr = elem.attrib['name']

            if attr in int_fields:
                key, sub_key = nested_keys(attr)
                if key not in doc:
                    doc[key] = {}

                if sub_key == "es":
                    continue

                doc[key][sub_key] = elem.text
            elif attr == "ranking":
                doc[attr] = int(elem.text)
            else:
                doc[attr] = elem.text

        elif elem.tag == 'doc':
            # document is finished, dump it to json
            if not ONLY_GERMANY or doc.get('country', {}).get('default') == "Deutschland":
                count += 1
                if not count % 1000:
                    print("progress: {:,}".format(count))

                f.write(INSERT_LINE)
                json.dump(doc, f)
                f.write("\n")

            doc = {}

        elem.clear()

    f.write("\n")

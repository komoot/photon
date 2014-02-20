# script to convert solr xml to check out other approaches

import xml.etree.cElementTree as et
from xml.sax.saxutils import escape


def get_search_field(doc, lang, housenumber_first=False):
    field = []
    field.append(doc.get('name_' + lang))
    field.append(doc.get('name'))

    if housenumber_first:
        field.append(doc.get('street'))
        field.append(doc.get('housenumber'))
    else:
        field.append(doc.get('street'))
        field.append(doc.get('housenumber'))

    field.append(doc.get('postcode'))
    field.append(doc.get('city_' + lang))
    field.append(doc.get('city'))
    field.append(doc.get('country_' + lang))
    field.append(doc.get('country'))

    places_local = doc.get('places_' + lang)
    if places_local:
        places_local = [t.strip() for t in places_local.split(',')]

    places = doc.get('places')
    if places:
        places = [t.strip() for t in places.split(',')]

    unified_places = set()

    if places_local:
        for p in places_local:
            unified_places.add(p)

    if places:
        for p in places:
            unified_places.add(p)

    if len(unified_places):
        field += list(unified_places)

    return "; ".join(filter(None, field))


input_xml = '/home/photon/data/solr-131012.xml'
#input_xml = 'sample_data/iceland.solr.xml'
xml_iter = et.iterparse(input_xml)

output_xml = input_xml.replace('.xml', '.extended.xml')

count = 0
with open(output_xml, 'w') as f:
    f.write("<?xml version='1.0' encoding='utf-8'?>\n<add>\n")

    doc = {}

    for event, elem in xml_iter:
        count += 1

        if not count % 100000:
            print("progress: {:,}".format(count))

        if elem.tag == 'doc':
            doc['search_de'] = get_search_field(doc, 'de', housenumber_first=True)
            doc['search_en'] = get_search_field(doc, 'en')
            doc['search_fr'] = get_search_field(doc, 'fr')
            doc['search_it'] = get_search_field(doc, 'it', housenumber_first=True)

            f.write("\t<doc>\n")

            for field in doc.items():
                if field[0] is None or field[1] is None:
                    continue

                f.write("""\t\t<field name="{0}">{1}</field>\n""".format(field[0], escape(field[1])))

            f.write("\t</doc>\n")

            doc = {}
        elif 'name' in elem.attrib:
            doc[elem.attrib['name']] = elem.text

            f.write("</add>")

        elem.clear()
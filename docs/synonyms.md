# Using Synonyms and Classification Terms

Photon has built-in support for using custom query-time synonyms and
special phrases for searching a place by its type. This document explains
how to configure this feature.

## Configuration

Synonyms and classification terms are configured with a JSON file which can
be added to a Photon server instance using the command line parameter
`-synonym-file`. Synonyms are a run-time feature. Handing in a synonym list
at import time has no effect. The list of synonyms in use can simply be
changed by restarting the Photon server with a different synonym list (or
not at all, if you want to completely disable the feature again).

Here is a simple example of a synonym configuration file:

```
{
  "search_synonyms": [
    "first,1st",
    "second,2nd"
  ],
  "classification_terms": [
    {
      "key": "aeroway",
      "value": "aerodrome",
      "terms": ["airport", "airfield"]
    },
    {
      "key": "railway",
      "value": "station",
      "terms": ["station"]
    }
  ]
}
```

The file has two main sections: `search_synonyms` allows for simple synonym
replacements in the query. `classification_term` defines descriptive terms
for a OSM key/value pair.

## Synonyms

The `search_synonyms` section must contain a list of synonym replacements.
Each entry contains a comma-separated of terms that may be replaced with each
other in the query. Only single-word terms are allowed. That means the terms
must neither contain spaces nor hyphens or the like.[^1]

[^1] This is a restriction of ElasticSearch 5. Synonym replacement does not
     create correct term positions when multi-word synonyms are involved.

## Classification Terms

The second section `classification_terms` defines a list of OSM key/value
pairs with their descriptive terms. `place` and `building` may not be used as
keys. Neither will `highway=residential` nor `highway=unclassified` work.
There may be multiple entries for the same key/value pair (for example,
if you have extra entries for each supported language).

The classification terms can help improve search when the type of an object
is used in the query but does not appear in the name. For example, with the
configuration given above a query of "Berlin Station" will find a railway
station which in OpenStreetMap has the name "Berlin" and also one with
the name "Berlin Hauptbahnhof".

Classification terms do not enable searching for objects of a certain type.
"Station London" will not get you all railway stations in London but a
railway station _named_ London.

## Usage Advice

Use synonyms and classification terms sparingly and only if you can be
reasonably sure that they will target the intended part of the address.
Short or frequent terms can have unexpected side-effects and worsen the
search results. For example, it might sound like a good idea to use synonyms
to handle the abbreviation from 'Saint' to 'St'. The problem here is that
'St' is also used as an abbreviation for 'Street'. So all searches that
involve a 'Street' will suddenly also search for places containing 'Saint'.

Do not create synonyms for terms that are used as classification terms.
Photon will not complain but again there might be unintended side effects.


# Using structured queries

The OpenSearch version of Photon has a separate endpoint for structured queries.
Structured queries make it possible to search for specific
countrycode / city / postcode / street / ... instead of the default query string.
If the address fields are known, structured queries often lead to better
results than the construction of a query string followed by a free text search.

## Enabling support

**Starting from Photon 1.0, structured queries are always available.**

Use "-structured" when importing the nominatim database. This option increases the index size by around 10%.

The information whether the data was imported with structured query support is stored within the OpenSearch index.

On startup photon checks whether the index supports structured queries and if so 
```
http://localhost:2322/structured?city=berlin
```
is available.

## Usage

Supported parameters are
```
"lang", "limit",  "lon", "lat", "osm_tag", "location_bias_scale", "bbox", "debug", "zoom", "layer", "countrycode", "state", "county", "city", "postcode", "district", "housenumber", "street"
```

countrycode has to be a valid ISO 3166-1 alpha-2 code (also known as ISO2).
All parameters shared with /api have the same meaning.

The result format is the same as for /api.

## Known issues

* state information is used with low priority. This can cause issues with cities that exist in several states (e.g. "Springfield" in the US). The reason is that states are not normalized - some documents have abbreviations like "NY", other spell "New York" out.
* abbreviations like the English 'Ave' (Avenue) and the German "Str." (Straße) are not supported.

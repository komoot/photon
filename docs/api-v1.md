# photon API

photon provides the following endpoints:

* [/api](#search) for forward search (finding a place by its name and address)
* [/structured](#structured-search) for finding places by a well-formatted address
* [/reverse](#reverse) for reverse geocoding (finding out what is at a given coordinate)
* [/status](#status) as a health check of the server
* (optional) `/update` endpoint for triggering updates,
  see [Usage/updates](usage.md#running-updates-via-the-api)
* (optional) `/metrics` for providing performance metrics for Prometheus

## Search

A simple forward search for a place looks like this:

```
http://localhost:2322/api?q=berlin
```

The **q** parameter contains the term to search for. It is mandatory unless
filtering via include/exclude parameters is in place.

Apart from the [common parameters](#parameters-common-to-search-and-reverse)
the `/api` endpoints accepts the following parameters:

#### Location Bias

```
http://localhost:2322/api?q=berlin&lon=10&lat=52&zoom=12&location_bias_scale=0.1
```

Use the **lat** and **lon** parameters to set a focus point for the search
where results should be preferred.

There are two optional parameters to influence the location bias. **zoom**
describes the radius around the center to focus on. This is a number that
should correspond roughly to the map zoom parameter of a corresponding map.
The default zoom is 16.

The **location_bias_scale** describes how much the prominence of a result should
still be taken into account. Sensible values go from 0.0 (ignore prominence
almost completely) to 1.0 (prominence has approximately the same influence).
The default is 0.2.

#### Filter results by bounding box

```
http://localhost:2322/api?q=berlin&bbox=9.5,51.5,11.5,53.5
```

The **bbox** parameter restricts results to the given area.
The expected format for the bounding box is minLon,minLat,maxLon,maxLat.

#### Filter results by country code

```
http://localhost:2322/api?q=berlin&countrycode=DE
```

The **countrycode** parameter restricts results to the given countries. 
The country code parameter can be used multiple times.
The expected format for the country code is the 2 letter code for the country 
also known as ISO 3166-2.

## Structured Search

```
http://localhost:2322/structured?city=berlin&street=Unter%20den%20Linden&housenumber=2
```

Structured search works similar to forward search but the query term is split
up into address parts. Sometimes this gives more targeted results when geocoding
addresses. The following query parameters are supported:
**countrycode**, **state**, **county**, **city**,
**postcode**, **district** (as in city district or suburb), **housenumber**,
**street**. The country code has to be a valid ISO 3166-1 alpha-2 code.

Other than that, all parameters that work for the /api endpoint are supported
with structured search as well.


## Reverse

```
http://localhost:2322/reverse?lon=10&lat=52&radius=10
```

The mandatory **lat** and **lon** parameters describe the coordinate which
to look up the location description for.
The optional **radius** parameter can be used to specify a value in kilometers
to reverse geocode within. The value has to be between 0 and 5000 km.

The /reverse call can be customized with the
[common parameters](#parameters-common-to-search-and-reverse).

## Parameters common to Search and Reverse

The following parameters work for search, reverse search and
structured search.

#### Number of Results

```
http://localhost:2322/api?q=berlin&limit=2
```

The **limit** parameter sets the number of results the server should return.
It may return less if there are not enough suitable results or when the
limit parameter exceeds the maximum number of requests the server is willing
to return. This limit is configurable and may differ between photon instances.

#### Adjust Language

```
http://localhost:2322/api?q=berlin&lang=it
```

The **lang** parameter defines in which language results should be returned
if possible. Only one language can be given here. If no translation for the
language exists, the server will fall back to returning the local language.
When the parameter is omitted the
['accept-language' HTTP header](https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Accept-Language)
will be used (browsers set this by default).

If neither is set the server-define default language is returned. This is
usually the local name of the place. In OpenStreetMap
data that's usually the value of the `name` tag,
for example the local name for Tokyo is 東京都.

#### Filter results by [tags and values](https://taginfo.openstreetmap.org/projects/nominatim#tags)

_Note: the filter only works on principal OSM tags and not all
OSM tag/value combinations can be searched. The actual list depends on the
import style used for the Nominatim database
(e.g. [settings/import-full.style](https://github.com/osm-search/Nominatim/blob/master/settings/import-full.style)).
All tag/value combinations with a property 'main' are included in the photon database._

If one or many query parameters named **osm_tag** are present, photon will
attempt to filter results by those tags. In general, here is the expected
format (syntax) for the value of osm_tag request parameters.

1. Include places with tag: `osm_tag=key:value`
2. Exclude places with tag: `osm_tag=!key:value`
3. Include places with tag key: `osm_tag=key`
4. Include places with tag value: `osm_tag=:value`
5. Exclude places with tag key: `osm_tag=!key`
6. Exclude places with tag value: `osm_tag=:!value`

For example, to search for all places named `berlin` with tag
of `tourism=museum`, one should construct url as follows:

```
http://localhost:2322/api?q=berlin&osm_tag=tourism:museum
```

Or, just by they key

```
http://localhost:2322/api?q=berlin&osm_tag=tourism
```

You can also use this feature for reverse geocoding. Want to see the 5 pharmacies closest to a location ?

```
http://localhost:2322/reverse?lon=10&lat=52&osm_tag=amenity:pharmacy&limit=5
```

#### Filter results by layer

```
http://localhost:2322/api?q=berlin&layer=city&layer=locality
```

List of available layers:

- house
- street
- locality
- district
- city
- county
- state
- country
- other (e.g. natural features)

#### Filter results by category

Use **include** and **exclude** parameters to filter by category. What
categories are defined depends on the specific installation of photon.

See the [category documentation](categories.md) for more information.

#### Dedupe results

```
http://localhost:2322/api?q=berlin&dedupe=0
```

Sometimes you have several objects in OSM identifying the same place or object
in reality. The simplest case is a street being split into many different
OSM ways due to different characteristics.
photon will attempt to detect such duplicates and only return one match.
Setting the **dedupe** parameter to `0` disables this deduplication mechanism
and ensures that all results are returned.
By default, photon will attempt to deduplicate results which have the 
same name, postcode, and OSM value if exists.

## Results for Search and Reverse

photon returns a response in GeoJSON format. The properties returned
follow the specification of the
[GeocodeJson format](https://github.com/geocoders/geocodejson-spec/tree/master/draft)
with the following extra fields added:

* `extra` is an object containing any extra tags, if available.

Example response:

```
json
{
  "features": [
    {
      "properties": {
        "name": "Berlin",
        "state": "Berlin",
        "country": "Germany",
        "countrycode": "DE",
        "osm_key": "place",
        "osm_value": "city",
        "osm_type": "N",
        "osm_id": 240109189
      },
      "type": "Feature",
      "geometry": {
        "type": "Point",
        "coordinates": [13.3888599, 52.5170365]
      }
    },
    {
      "properties": {
        "name": "Berlin Olympic Stadium",
        "street": "Olympischer Platz",
        "housenumber": "3",
        "postcode": "14053",
        "state": "Berlin",
        "country": "Germany",
        "countrycode": "DE",
        "osm_key": "leisure",
        "osm_value": "stadium",
        "osm_type": "W",
        "osm_id": 38862723,
        "extent": [13.23727, 52.5157151, 13.241757, 52.5135972]
      },
      "type": "Feature",
      "geometry": {
        "type": "Point",
        "coordinates": [13.239514674078611, 52.51467945]
      }
    }
  ]
}
```

### Status

```
http://localhost:2322/status
```

returns a JSON document containing the status and the last update date of
the data. (That is the date, when the data is from, not when it was imported
into photon.)


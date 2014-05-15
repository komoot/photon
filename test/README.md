##### Notes:
Results are not tested against osm_type/osm_id so that it can be run against non-OSM geocoders as well.

Precise geographic location may be tricky to test against non-OSM geocoders, as they may differ (for example the center of a city is not an absolute truth, depending on the way you're calculating it). There is an "expected location tolerance" parameter that allows a tolerance (in meters) to support such cases in each test.

There is no testing around house numbers yet because of the scarcity of data sources.

##### Test queries format
The queries are written in separate CSV files. The files are using semicolons as separators, and are encoded as UTF-8. The schema used is: `comment;tried_query;tried_location;tried_language;expected_maximum_position;expected_location;expected_location_tolerance;expected_name;expected_type;expected_housenumber;expected_street;expected_city;expected_postcode;expected_country`.

Each test uses the `tried_*` parameters that are filled, and is checking the result against the `expected_*` parameters that are filled.

The `comment` field is optional and used to write comments about the specific test, for easier maintenance.

The `expected_type` should be ignored in Photon right now, until such a type is implemented.

### Test suites

##### Basic matching
This test suite tries basic and specific queries, to ensure that the search works at least at a simple level, and that the data is available. It tries to not consider relevance and boosting settings, as this is checked by other suites.
- Countries
- Cities
- Regions, departments, counties... various administrative boundaries
- Streets
- Landmarks
- Train stations

##### Fuzzy matching
This suite ensures that specific places are matched by "fuzzy" typing. The places tested can be taken from the "Basic matching" test suite, so that all types of objects are tested.

##### Location bias
This suite ensures that location bias effectively works. It tries to work against similar places (same type, same size...) to stay away from other criterias that would influence the scoring.

##### Boosting of relevant places
This suite ensures that some places are getting better scoring due to some of their attributes.
- Big cities should come before smaller cities
- Big regions (or other administrative boundary) should come before smaller regions
- Streets in big cities should come before streets in small cities
- Streets in big cities should come before small cities of the same name
- Famous landmarks should come before anything named after them (like streets)

##### Boosting versus location bias
This suite tries searches and expects results perceived as "obvious" in order to hit the sweet spot of ranking where the location bias can return a "smaller" object because we are close enough, despite a bigger object somewhere else being boosted for other reasons. In other words: if I'm searching for "belleville", I'm usually looking for "rue de belleville" in Paris... unless I'm in the country and looking for the small city of Belleville.

##### Preferred language
This suite tries the same search under different languages to ensure the preferred language correctly boost results.

##### Special phrases
cf http://wiki.openstreetmap.org/wiki/Nominatim/Special_Phrases
This suite ensures the "special phrase" handling is effectively working and returning results using that list.

##### Synonyms
This suite ensures the synonyms list is effectively working.

##### Suggest
This test suite ensures that the suggest works, by trying different combinations of prefixes (b, be, ber, berl...).
It also ensures that the scoring and boosting tweaks are applied to the suggest results.
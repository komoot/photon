# Categories

Categories allow to define custom filtering on top of a Photon geo database.
Each place in the Photon database can be assigned an arbitrary number of
categories. Then the "include" and "exclude" parameter can be used to filter
results for presence or absence of certain categories.

## Category definition

A category name consists of a sequence of labels which are separated by dots.
A label is an arbitrary string consisting of letters, numbers, underscore or
dash (or to be precise: `[a-zA-Z0-9_-]`). Category names are case-sensitive.

The leading label defines the _category group_, subsequent labels the value
within the group. Note that this means that your category must have at least
two components: the group and one value label.

The labels are considered to be forming a hierarchy. A
place can then be filtered by any part of the hierarchy. A depth between
1 and 4 labels is supported for filtering.

## Adding categories to a place

Categories can be added to a place via the JSON import. Add a field `categories`
containing an array of all categories, you want to add. Category names that
do not confirm to the category syntax are silently dropped.

The Nominatim database exporter creates exactly one category entry using
the (reserved) group `osm`. It contains the main tag key and value, the
same as the API returns in the result as `osm_key` and `osm_value`. _Note that
the exporter will replace key and value, when they do not conform to the
category syntax with `place` and `yes` respectively._

## Difference between categories and extra tags

Photon supports another field for custom values: `extra`. Extra values are
saved in the database as is and returned with the response. They are not
indexed and cannot be searched.

Categories, on the other hand, are _only_ usable for filtering. They are not
saved as raw data and not returned to the user.

This gives you fine-grained control over which extra information to return to
the user and what filters to support. If you have data which you want to return
_and_ want to filter by, simply add it twice: once in extra and again as a
category.

## Filtering with `include` and `exclude`

Searches and reverse searches can be filtered by category using the
`include` and `exclude` parameters.

The include parameter takes a comma-separated list of categories and will
include a place if any of the categories is present. The include parameter
may be repeated. The conditions must then be all fulfilled.

The exclude parameter also takes a comma-separated list and will exclude and
object when **all** categories are present. When repeated, the object will be
excluded, when any of the exclude conditions is met.

You can have partial label-paths in your filter condition. `include=food.shop`
will match `food.shop.supermarket` and `food.shop.convenience_store` as well.
However, you cannot filter by just the category group. A search with
`include=food` will return an error.

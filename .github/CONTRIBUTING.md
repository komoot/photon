Submit a new issue only if you are sure it is a missing feature or a bug. Otherwise please discuss the topic in the [mailing list](https://lists.openstreetmap.org/listinfo/photon) first. 

## We love pull requests. Here's a quick guide:

1. [Fork the repo](https://help.github.com/articles/fork-a-repo) and create a branch for your new feature or bug fix.

2. Run the tests. We only take pull requests with passing tests: `mvn clean test`

3. Add at least one test for your change. Only refactoring and documentation changes
require no new tests. Also make sure you submit a change specific to exactly one issue. If you have ideas for multiple 
changes please create separate pull requests.

4. Make the test(s) pass.

5. Push to your fork and [submit a pull request](https://help.github.com/articles/using-pull-requests). A button should
appear on your fork its github page afterwards.

## Code formatting

We use IntelliJ defaults and a very similar configuration for NetBeans defined in the root pom.xml. For eclipse there is this [configuration](https://github.com/graphhopper/graphhopper/files/481920/GraphHopper.Formatter.zip). Also for other IDEs 
it should be simple to match:

 * Java indent is 4 spaces
 * Line width is 100 characters
 * The rest is left to Java coding standards but disable "auto-format on save" to prevent unnecessary format changes. 
 * Currently we do not care about import section that much, avoid changing it
 * Unix line endings (should be handled via git)

And in case we didn't emphasize it enough: we love tests!

## Changes to mapping and index settings

It is possible to change the mapping layout or index settings any time as
long as they are compatible with the current layout. Photon reapplies the
mapping and index settings on startup to make sure it conforms to the latest
code.

**Warning:** the kind of modifications that can be done on an existing
index are limited. Always test if your modifications are compatible by importing
a database with the version before your changes and then running Photon with
the version with your changes applied.

If the mappings or the settings are changed in an incompatible way that
requires a reimport, then you must increase the database version in
`src/main/java/de/komoot/photon/elasticsearch/DatabaseProperties.java`.
For major/minor/patch always use the version of the next release. If the
version number already points to the next release, increase the dev-version.

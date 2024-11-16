CREATE TABLE placex (
  place_id BIGINT NOT NULL,
  parent_place_id BIGINT,
  linked_place_id BIGINT,
  importance FLOAT,
  indexed_date TIMESTAMP,
  geometry_sector INTEGER,
  rank_address SMALLINT,
  rank_search SMALLINT,
  partition SMALLINT,
  indexed_status SMALLINT,
  osm_id int8 NOT NULL,
  osm_type char(1) NOT NULL,
  class text NOT NULL,
  type text NOT NULL,
  name JSON,
  admin_level smallint,
  address JSON,
  extratags JSON,
  geometry Geometry,
  wikipedia TEXT, -- calculated wikipedia article name (language:title)
  country_code varchar(2),
  housenumber TEXT,
  postcode TEXT,
  centroid GEOMETRY
);

CREATE TABLE place_addressline (
  place_id BIGINT,
  address_place_id BIGINT,
  distance FLOAT,
  cached_rank_address SMALLINT,
  fromarea boolean,
  isaddress boolean
);

CREATE TABLE location_property_osmline (
    place_id BIGINT NOT NULL,
    osm_id BIGINT,
    parent_place_id BIGINT,
    geometry_sector INTEGER,
    indexed_date TIMESTAMP,
    startnumber INTEGER,
    endnumber INTEGER,
    step SMALLINT,
    partition SMALLINT,
    indexed_status SMALLINT,
    linegeo GEOMETRY,
    address JSON,
    postcode TEXT,
    country_code VARCHAR(2)
);


CREATE ALIAS ST_Envelope FOR "de.komoot.photon.nominatim.testdb.Helpers.envelope";

CREATE TABLE country_name (
    country_code character varying(2),
    name JSON,
    country_default_language_code character varying(2),
    partition integer
);

INSERT INTO country_name
    VALUES ('de', JSON '{"name" : "Deutschland", "name:en" : "Germany"}', 'de', 2);
INSERT INTO country_name
    VALUES ('us', JSON '{"name" : "USA", "name:en" : "United States"}', 'en', 1);
INSERT INTO country_name
    VALUES ('hu', JSON '{"name" : "Magyarország", "name:en" : "Hungary"}', 'hu', 12);
INSERT INTO country_name
    VALUES ('nl', JSON '{"name" : "Nederland", "name:en" : "Netherlands"}', null, 2);


CREATE TABLE photon_updates (
    rel TEXT,
    place_id BIGINT,
    operation TEXT,
    indexed_date TIMESTAMP
);


CREATE TABLE import_status (
    lastimportdate timestamp with TIME ZONE NOT NULL,
    indexed boolean
);
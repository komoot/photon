DROP TABLE location_postcodes;

CREATE TABLE location_postcode (
    place_id BIGINT,
    parent_place_id BIGINT,
    rank_search SMALLINT,
    rank_address SMALLINT,
    indexed_status SMALLINT,
    country_code VARCHAR(2),
    postcode TEXT,
    geometry GEOMETRY
);
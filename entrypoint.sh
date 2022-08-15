#!/bin/bash
# Inspired by https://github.com/thomasnordquist/photon-docker

# Extract elasticsearch index
if [ ! -d "/photon/photon_data/elasticsearch" ]; then
  echo "Search index not found"
  if [ ! -f "/photon/photon_data/photon-db-latest.tar.xz" ]; then
    echo "Couldn't find photon-db-latest.tar.xz."
    echo "Please add it to the photon_data volume."
  fi
  echo "Extract search index. This may take a while."
  pv /photon/photon_data/photon-db-latest.tar.xz | tar xvj --strip-components=1 -C /photon/photon_data/

fi

# Start photon if elastic index exists
if [ -d "/photon/photon_data/elasticsearch" ]; then
    echo "Start photon"
    java -jar photon.jar $@
else
    echo "Could not start photon, the search index could not be found"
fi
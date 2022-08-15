#!/bin/bash
# Inspired by https://github.com/thomasnordquist/photon-docker

# Extract elasticsearch index
if [ ! -d "/photon/photon_data/elasticsearch" ]; then
  echo
  if [ ! -f "/photon/photon_data/photon-db-latest.tar.xz" ]; then
    echo "Couldn't find photon-db-latest.tar.xz."
    echo "Please add it to the photon_data volume."
  fi
  echo "Extract search index"
  tar xf photon-db-latest.tar.xz
fi

# Start photon if elastic index exists
if [ -d "/photon/photon_data/elasticsearch" ]; then
    echo "Start photon"
    java -jar photon.jar $@
else
    echo "Could not start photon, the search index could not be found"
fi
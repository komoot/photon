#!/bin/bash
# Inspired by https://github.com/thomasnordquist/photon-docker

#
# Extract elasticsearch index
if [ ! -f "/photon/photon_data/photon-db-latest.tar.bz2" ]; then
    echo "No suitable photon db found. Please check the data folder."
    exit 1
else
    echo "Extract search index"
    bzip2 -cd photon-db-latest.tar.bz2 | tar x -C photon_data/
fi

# Extract elasticsearch index
if [ ! -d "/photon/photon_data/elasticsearch" ]; then
    echo "Extract search index"
    bzip2 -cd photon-db-latest.tar.bz2 | tar x -C photon_data/
fi

# Start photon if elastic index exists
if [ -d "/photon/photon_data/elasticsearch" ]; then
    echo "Start photon"
    java -jar photon.jar $@
else
    echo "Could not start photon, the search index could not be found"
fi
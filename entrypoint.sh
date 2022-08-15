#!/bin/bash
# Inspired by https://github.com/thomasnordquist/photon-docker

# Check if index is up to date, if not start from scratch.
if [ -f "/photon/photon_data/photon-db-latest.tar.xz" ]; then
    echo "Found photon-db-latest.tar.xz."
    if [ -f "/photon/photon_data/file-hash.md5" ]; then
      echo "Checking validity."
      if ! md5sum -c file-hash.md5; then
        echo "Current photon db out of date. Starting fresh."
        rm -rf /photon/photon_data/elasticsearch
        rm -rf /photon/photon_data/file-hash.md5
      fi
    else
        echo "No extracted photon db. Starting fresh."
        rm -rf /photon/photon_data/elasticsearch
        rm -rf /photon/photon_data/file-hash.md5
    fi
  else
    echo "Couldn't find photon-db-latest.tar.xz"
    echo "Please add it to the photon_data volume."
    exit 1
fi

# Extract elasticsearch index
if [ ! -d "/photon/photon_data/elasticsearch" ]; then
  echo "Search index not found"
  echo "Extract search index. This may take a while."
  (pv --force "/photon/photon_data/photon-db-latest.tar.xz" | tar xp -J --strip-components=1 -C /photon/photon_data/) 2>&1 | stdbuf -o0 tr '\r' '\n'
fi

# Start photon if elastic index exists
if [ -d "/photon/photon_data/elasticsearch" ]; then
    echo "Start photon"
    java -jar photon.jar $@
else
    echo "Could not start photon, the search index could not be found."
    echo "Check the photon-db-latest.tar.xz for errors."
fi
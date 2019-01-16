#!/bin/bash

# Download INRIA Holidays dataset
mkdir -p dataset && cd dataset
curl -O ftp://ftp.inrialpes.fr/pub/lear/douze/data/jpg1.tar.gz
tar -xzvf jpg1.tar.gz

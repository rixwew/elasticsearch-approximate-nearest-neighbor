#!/bin/bash

# download and unzip insurance qa dataset
git submodule update --recursive

# download pretrained word2vec model
curl -O https://s3.amazonaws.com/dl4j-distribution/GoogleNews-vectors-negative300.bin.gz
gzip -d GoogleNews-vectors-negative300.bin.gz

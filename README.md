# Elasticsearch Approximate Nearest Neighbor plugin

This repository provides product quantization based approximate nearest neighbor elasticsearch plugin for searching high-dimensional dense vectors.

It can be used for various purposes with neural network frameworks (like tensorflow, pytorch, etc).  
For example,  
1. Similar images search
2. Question answering
3. Recommendation or learning to rank

(See [examples](./examples))

Product quantization implementation is based on paper "Product quantization for nearest neighbor search" - Herve Jegou, Matthijs Douze, Cordelia Schmid.

## Installation

See the full list of prebuilt versions. If you don"t see a version available, see the link below for building or file a request via issues.

To install, you"d run a command such as:

    ./bin/elasticsearch-plugin install http://${release_zip_file}

After installation, you need to restart running Elasticsearch.


## Usage

This plugin provides custom mapper type, analyzer, and search query.  

### Create mapping

Before you create mapping, product quantizer parameters has to be trained.  
See [example](./examples/lib/common.py) code.

    PUT sample_images

```json
{
  "settings": {
    "analysis": {
      "analyzer": {
        "image_analyzer": {
          "type": "ivfpq_analyzer",
          "d": 256,
          "m": 128,
          "ksub": 64,
          "coarseCentroids": "0.007750325836241245,0.0010391526157036424,...,0.031184080988168716",
          "pqCentroids": "0.00041024317033588886,0.022187601774930954,...,0.001461795181967318",
        }
      }
    }
  },
  "mappings": {
    "vector": {
      "_source": {
        "excludes": [
          "feature"
        ]
      },
      "properties": {
        "name": {
          "type": "keyword"
        },
        "feature": {
          "type": "ivfpq",
          "analyzer": "image_analyzer"
        }
      }
    }
  }
}
```


### Index vector data

The following example adds or updates a vector data in a specific index, making it searchable.

    POST sample_images/image/1
    
```json
{
  "name": "1.jpg",
  "feature": "0.018046028912067413,0.0010425627697259188,...,0.0012223172234371305"
}
```

### Search similar vectors

The ivfpq_query within the search request body could be used with other elasticsearch queries.

    POST sample_images/_search

```json
{
  "query": {
    "ivfpq_query": {
      "query": "",
      "fields": ["feature"]
    }
  },
  "sort": [
    {"_score": {"order": "asc"}}
  ]
}
```

## Development

If you want to build for a new elasticsearch version which is not released, you could build by the following way.

### 1. Build with Gradle Wrapper

    ./gradlew build

This command generates a Elasticsearch plugin zip file.


### 2. Install with ./bin/elasticsearch-plugin

    ./bin/elasticsearch-plugin install file:///${path_to_generated_zip_file}

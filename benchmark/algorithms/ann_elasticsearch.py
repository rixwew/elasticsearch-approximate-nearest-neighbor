from __future__ import absolute_import

import subprocess
import sys
import time

import elasticsearch
import elasticsearch.helpers
import sympy
from tqdm import tqdm

sys.path.append("install/lib-faiss")
import numpy
import sklearn.preprocessing
import faiss
from ann_benchmarks.algorithms.base import BaseANN


class ANNElasticsearch(BaseANN):

    def __init__(self, metric, n_list):
        self.n_list = n_list
        self.n_probe = None
        self.metric = metric
        self.run_server()
        self.client = elasticsearch.Elasticsearch()

    def query(self, v, n):
        if self.metric == 'angular':
            v /= numpy.linalg.norm(v)
        response = self.client.search('vectors', body={
            'query': {
                'ivfpq_query': {
                    'query': ','.join(map(str, v)),
                    'fields': ['vector'],
                    'nprobe': self.n_probe
                }
            },
            "sort": [
                {"_score": {"order": "asc"}},
            ],
            'size': n
        })
        return [int(hit['_id']) for hit in response['hits']['hits']]

    def fit(self, X):
        if self.metric == 'angular':
            X = sklearn.preprocessing.normalize(X, axis=1, norm='l2')
        if X.dtype != numpy.float32:
            X = X.astype(numpy.float32)
        d = X.shape[1]
        m = self.get_subvector_size(d)
        quantizer = faiss.IndexFlatL2(d)
        index = faiss.IndexIVFPQ(quantizer, d, self.n_list, m, 8)
        index.train(X)
        coarse_centroids = [quantizer.xb.at(i) for i in range(quantizer.xb.size())]
        pq_centroids = [index.pq.centroids.at(i) for i in range(index.pq.centroids.size())]
        self.create_mapping(d, m, index.pq.ksub, coarse_centroids, pq_centroids)
        self.add_vectors(X)

    def set_query_arguments(self, n_probe):
        self.n_probe = n_probe

    def run_server(self):
        subprocess.call(['bash', './elasticsearch/bin/start.sh'])
        time.sleep(10)

    def get_subvector_size(self, d, k=64):
        dsub = 1
        factors = sympy.ntheory.factorint(d)
        for p, n in sorted(factors.items(), key=lambda x: -x[0]):
            for i in range(n):
                dsub *= p
                if dsub >= k:
                    break
        return d // dsub

    def create_mapping(self, d, m, ksub, coarse_centroids, pq_centroids):
        self.client.indices.create('vectors', {
            'settings': {
                'analysis': {
                    'analyzer': {
                        'ann_analyzer': {
                            'type': 'ivfpq_analyzer',
                            'd': d,
                            'm': m,
                            'ksub': ksub,
                            'coarseCentroids': ','.join(map(str, coarse_centroids)),
                            'pqCentroids': ','.join(map(str, pq_centroids))
                        }
                    }
                }
            },
            'mappings': {
                'vector': {
                    "_source": {
                        "enabled": False
                    },
                    'properties': {
                        'vector': {
                            'type': 'ivfpq',
                            'analyzer': 'ann_analyzer'
                        }
                    }
                }
            }
        })

    def add_vectors(self, vectors, batch_size=1024):
        for i in tqdm(range(0, vectors.shape[0], batch_size)):
            actions = [{
                '_index': 'vectors',
                '_type': 'vector',
                '_id': i + j,
                '_source': {
                    'vector': ','.join(map(str, vector))
                }
            } for j, vector in enumerate(vectors[i:i+batch_size])]
            elasticsearch.helpers.bulk(self.client, actions)
        print('create index: finish:', self.client.count(index='vectors', doc_type='vector'))

    def __str__(self):
        return 'ANN-Elasticsearch(n_list=%d, n_probe=%d)' % (self.n_list, self.n_probe)

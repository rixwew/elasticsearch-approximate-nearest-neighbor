import faiss


def fit_pq_params(xb, d, nlist, m):
    quantizer = faiss.IndexFlatL2(d)
    index = faiss.IndexIVFPQ(quantizer, d, nlist, m, 4)
    index.train(xb)
    coarse_centroids = [quantizer.xb.at(i) for i in range(quantizer.xb.size())]
    pq_centroids = [index.pq.centroids.at(i) for i in range(index.pq.centroids.size())]
    return coarse_centroids, pq_centroids, index.pq.ksub, index.pq.dsub


class SearchClient(object):

    def __init__(self, client, index_name, type_name):
        self.client = client
        self.index_name = index_name
        self.type_name = type_name

    def add_vectors(self, descriptions, feats):
        for i in range(len(descriptions)):
            description = descriptions[i]
            feat = feats[i].tolist()
            doc = {
                'description': description,
                'vector': ','.join(map(str, feat))
            }
            self.client.index(index=self.index_name,
                              doc_type=self.type_name,
                              body=doc)

    def query(self, feat, result_size=10):
        query = {
            'query': {
                'ivfpq_query': {
                    'query': ','.join(map(str, feat)),
                    'fields': ['vector']
                }
            },
            'sort': {'_score': {'order': 'asc'}},
            'size': result_size
        }
        response = self.client.search(self.index_name, body=query)
        return response

    def create_mapping(self, d, m, ksub, coarse_centroids, pq_centroids):
        body = {
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
                self.type_name: {
                    "_source": {
                        "excludes": [
                            "vector"
                        ]
                    },
                    'properties': {
                        'description': {
                            'type': 'keyword'
                        },
                        'vector': {
                            'type': 'ivfpq',
                            'analyzer': 'ann_analyzer'
                        }
                    }
                }
            }
        }
        self.client.indices.create(self.index_name, body)

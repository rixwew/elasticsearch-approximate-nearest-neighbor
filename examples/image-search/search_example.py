import json
from pathlib import Path

import cv2
import elasticsearch
import numpy as np
import torch
from common import SearchClient, fit_pq_params

from models import ImageEncoder


def get_features_batch(encoder, images, use_cuda):
    with torch.no_grad():
        x = torch.FloatTensor(images).transpose(1, 3) / 255
        if use_cuda:
            x = x.cuda()
        return encoder(x).cpu().numpy()


def get_features(image_encoder, image_path_iter, use_cuda, batch_size=64):
    feats, names, images = list(), list(), list()
    for image_path in image_path_iter:
        name = str(image_path)
        names.append(name)
        image = cv2.imread(name)
        image = cv2.resize(image, (224, 224))
        images.append(image)
        if len(images) == batch_size:
            feats.append(get_features_batch(image_encoder, images, use_cuda))
            images = list()
    if len(images) > 0:
        feats.append(get_features_batch(image_encoder, images, use_cuda))
    return names, np.concatenate(feats, axis=0)


def main(query, result_size, dataset_path, nlist, m, use_cuda):
    image_encoder = ImageEncoder().eval()
    if use_cuda:
        image_encoder = image_encoder.cuda()
    es = elasticsearch.Elasticsearch()
    client = SearchClient(es, index_name='images', type_name='image')
    names, feats = get_features(image_encoder, Path(dataset_path).iterdir(), use_cuda)
    coarse_centroids, pq_centroids, ksub, dsub = fit_pq_params(feats, feats.shape[1], nlist, m)
    client.create_mapping(feats.shape[1], m, ksub, coarse_centroids, pq_centroids)
    client.add_vectors(names, feats)
    _, encoded_query = get_features(image_encoder, [query], use_cuda, batch_size=1)
    result = client.query(encoded_query[0], result_size)
    print(json.dumps(result, indent=2))


if __name__ == '__main__':
    import argparse

    parser = argparse.ArgumentParser()
    parser.add_argument('--query', required=True)
    parser.add_argument('--result_size', type=int, default=5)
    parser.add_argument('--dataset', default='dataset/jpg')
    parser.add_argument('--nlist', type=int, default=8)
    parser.add_argument('--m', type=int, default=64)
    parser.add_argument('--use_cuda', type=bool, default=True)
    args = parser.parse_args()

    main(query=args.query,
         result_size=args.result_size,
         dataset_path=args.dataset,
         nlist=args.nlist,
         m=args.m,
         use_cuda=args.use_cuda)

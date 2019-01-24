import json

import elasticsearch
import numpy as np
import torch
from torch.nn.utils.rnn import pad_sequence

from dataset import AnswerData
from common import SearchClient, fit_pq_params


def get_vid_surf_mappers(vocab_data_path):
    vid2surf = dict()
    with open(vocab_data_path, encoding='utf-8') as f:
        for line in f:
            vid, surf = line.rstrip().split('\t')
            vid2surf[vid] = surf
    return vid2surf, {surf: vid for vid, surf in vid2surf.items()}


def get_features(model, word_ids_list, use_cuda, batch_size=64):
    feats = list()
    for batch_i in range(0, len(word_ids_list), batch_size):
        batch = word_ids_list[batch_i:batch_i + batch_size]
        with torch.no_grad():
            batch = pad_sequence([torch.LongTensor(x) for x in batch], batch_first=True)
            if use_cuda:
                batch = batch.cuda()
            feat = model(batch)
            norm = feat.norm(dim=1, keepdim=True)
            feat = feat.div(norm.expand_as(feat))
            feats.append(feat.cpu().numpy())
    return np.concatenate(feats, axis=0)


def main(question,
         result_size,
         answer_data_path,
         vocab_data_path,
         model_path,
         nlist,
         m,
         max_length,
         use_cuda):
    # load model
    state = torch.load(model_path)
    model = state['model'].eval()
    if not use_cuda:
        model = model.cpu()
    else:
        model = model.cuda()
    vocab = state['vocab']

    vid2surf, surf2vid = get_vid_surf_mappers(vocab_data_path)
    answer_data = AnswerData(answer_data_path)
    answer_surfaces = list()
    answer_wids = list()
    for answer_vids in answer_data.answers.values():
        answer_surfaces.append(' '.join([vid2surf.get(vid) for vid in answer_vids]))
        answer_wids.append(vocab.word_ids(answer_vids[:max_length]))

    question_wids = vocab.word_ids([surf2vid.get(surf, '_unk') for surf in question.split(' ')])
    es = elasticsearch.Elasticsearch()
    client = SearchClient(es, index_name='answers', type_name='answer')
    feats = get_features(model, answer_wids, use_cuda)
    coarse_centroids, pq_centroids, ksub, dsub = fit_pq_params(feats, feats.shape[1], nlist, m)
    client.create_mapping(feats.shape[1], m, ksub, coarse_centroids, pq_centroids)
    client.add_vectors(answer_surfaces, feats)
    encoded_query = get_features(model, [question_wids], use_cuda, batch_size=1)
    result = client.query(encoded_query[0], result_size)
    print(json.dumps(result, indent=2))


if __name__ == '__main__':
    import argparse

    parser = argparse.ArgumentParser()
    parser.add_argument('--question', required=True)
    parser.add_argument('--result_size', type=int, default=5)
    parser.add_argument('--answer_data_path', default='dataset/V1/answers.label.token_idx')
    parser.add_argument('--vocab_data_path', default='dataset/V1/vocabulary')
    parser.add_argument('--model_path', default='./model.pt')
    parser.add_argument('--nlist', type=int, default=64)
    parser.add_argument('--m', type=int, default=47)
    parser.add_argument('--max_length', type=int, default=200)
    parser.add_argument('--use_cuda', type=bool, default=True)
    args = parser.parse_args()

    main(question=args.question,
         result_size=args.result_size,
         answer_data_path=args.answer_data_path,
         vocab_data_path=args.vocab_data_path,
         model_path=args.model_path,
         nlist=args.nlist,
         m=args.m,
         max_length=args.max_length,
         use_cuda=args.use_cuda)

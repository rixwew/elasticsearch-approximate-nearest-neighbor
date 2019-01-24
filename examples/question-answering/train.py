import argparse

import gensim
import numpy as np
import torch
import torch.utils.data
from torch.nn.utils.rnn import pad_sequence
from tqdm import tqdm

from dataset import InsuranceQaDataset, Vocab, AnswerData, QaData
from loss import QaLoss
from models import SentenceEncoder


def train(model,
          train_data_loader,
          test_dataset,
          optimizer,
          criterion,
          epoch,
          use_cuda,
          checkpoint=50):
    for epoch_i in range(epoch):
        model = model.train()
        total_loss = 0
        for question, positive, negative in tqdm(train_data_loader):
            if use_cuda:
                question, positive, negative = \
                    question.cuda(), positive.cuda(), negative.cuda()
            q_embed, p_embed, n_embed = model(question), model(positive), model(negative)
            loss = criterion(q_embed, p_embed, n_embed)

            optimizer.zero_grad()
            loss.backward()
            optimizer.step()
            total_loss += loss.item()
        print('epoch:', epoch_i, 'loss:', total_loss / len(train_data_loader))
        if (epoch_i + 1) % checkpoint == 0:
            test(model, test_dataset, use_cuda)


def test(model, test_dataset, use_cuda):
    model = model.eval()
    accuracy = 0
    with torch.no_grad():
        for i in tqdm(range(len(test_dataset))):
            question, positives, negatives = test_dataset.get_qa_entry(i)
            question = torch.LongTensor(question).unsqueeze(0)
            positives = pad_sequence([torch.LongTensor(x) for x in positives], batch_first=True)
            negatives = pad_sequence([torch.LongTensor(x) for x in negatives], batch_first=True)
            if use_cuda:
                question, positives, negatives = \
                    question.cuda(), positives.cuda(), negatives.cuda()
            q_embed, p_embed, n_embed = model(question), model(positives), model(negatives)

            p_sims = (q_embed * p_embed).sum(1, keepdim=True)
            n_sims = (q_embed * n_embed).sum(1, keepdim=True)
            result = list()
            result.extend([(score, 0) for score in n_sims])
            result.extend([(score, 1) for score in p_sims])
            accuracy += sorted(result, key=lambda x: -x[0])[0][1]
    print('test dataset: top1 accuracy:', accuracy / len(test_dataset))


def main(train_data_path,
         test_data_path,
         answer_data_path,
         vocab_data_path,
         embedding_data_path,
         batch_size,
         learning_rate,
         hidden_size,
         margin,
         epoch,
         save_path,
         pretrained_path,
         use_cuda):
    # load qa data
    answer_data = AnswerData(answer_data_path)
    train_data = QaData(train_data_path)
    test_data = QaData(test_data_path)

    # load pretrained embedding
    pretrained_embedding = gensim.models.KeyedVectors.load_word2vec_format(
        embedding_data_path, binary=True)
    vocab = Vocab(vocab_data_path, answer_data.lexicon + train_data.lexicon)
    pretrained_weights = np.zeros((len(vocab) + 1, 300)) # TODO magic number
    for wid, surf in vocab.wid2surf.items():
        if surf in pretrained_embedding.vocab:
            pretrained_weights[wid] = pretrained_embedding.wv[surf]

    # create dataset / data loader
    train_dataset = InsuranceQaDataset(train_data, answer_data, vocab)
    train_data_loader = torch.utils.data.DataLoader(train_dataset,
                                                    shuffle=True,
                                                    batch_size=batch_size,
                                                    collate_fn=train_dataset.collate)
    test_dataset = InsuranceQaDataset(test_data, answer_data, vocab)

    # train model
    if pretrained_path is not None:
        model = torch.load(pretrained_path)['model']
    else:
        model = SentenceEncoder(pretrained_weights, hidden_size)
    optimizer = torch.optim.Adam(params=model.parameters(), lr=learning_rate)
    criterion = QaLoss(margin=margin)
    if use_cuda:
        model = model.cuda()
    train(model, train_data_loader, test_dataset, optimizer, criterion, epoch, use_cuda)

    # save model
    torch.save({
        'model': model,
        'vocab': vocab
    }, save_path)


if __name__ == '__main__':
    parser = argparse.ArgumentParser()
    parser.add_argument('--train_data_path', default='dataset/V1/question.train.token_idx.label')
    parser.add_argument('--test_data_path', default='dataset/V1/question.dev.label.token_idx.pool')
    parser.add_argument('--answer_data_path', default='dataset/V1/answers.label.token_idx')
    parser.add_argument('--vocab_data_path', default='dataset/V1/vocabulary')
    parser.add_argument('--embedding_data_path', default='./GoogleNews-vectors-negative300.bin')
    parser.add_argument('--batch_size', type=int, default=128)
    parser.add_argument('--learning_rate', type=float, default=0.0005)
    parser.add_argument('--epoch', type=int, default=100)
    parser.add_argument('--hidden_size', type=int, default=141)
    parser.add_argument('--loss_margin', type=float, default=0.2)
    parser.add_argument('--save_path', default='./model.pt')
    parser.add_argument('--pretrained_path', default=None)
    parser.add_argument('--use_cuda', type=bool, default=True)
    args = parser.parse_args()

    main(train_data_path=args.train_data_path,
         test_data_path=args.test_data_path,
         answer_data_path=args.answer_data_path,
         vocab_data_path=args.vocab_data_path,
         embedding_data_path=args.embedding_data_path,
         batch_size=args.batch_size,
         learning_rate=args.learning_rate,
         hidden_size=args.hidden_size,
         margin=args.loss_margin,
         epoch=args.epoch,
         save_path=args.save_path,
         pretrained_path=args.pretrained_path,
         use_cuda=args.use_cuda)

import collections

import numpy
import torch.utils.data


class Vocab(object):

    def __init__(self, vocab_path, lexicon, unk_surf='<UNK>', thresh=5):
        self.vid2surf = dict()
        lexicon = {vocab_id for vocab_id, count in lexicon.items() if count >= thresh}
        with open(vocab_path, encoding='utf-8') as f:
            for _line in f:
                vocab_id, surf = _line.rstrip().split('\t')
                if vocab_id in lexicon:
                    self.vid2surf[vocab_id] = surf
        self.vid2wid = {vocab_id: i + 1 for i, vocab_id in enumerate(self.vid2surf)}
        self.wid2surf = {self.vid2wid.get(vid): surf for vid, surf in self.vid2surf.items()}
        self.unk_surf = unk_surf
        self.unk_word_id = len(self.vid2wid) + 1

    def surfaces(self, vocab_ids):
        return [self.vid2surf.get(vocab_id, self.unk_surf) for vocab_id in vocab_ids]

    def word_ids(self, vocab_ids):
        return [self.vid2wid.get(vocab_id, self.unk_word_id) for vocab_id in vocab_ids]

    def __len__(self):
        return len(self.vid2surf) + 1


class AnswerData(object):

    def __init__(self, data_path):
        self.answers = dict()
        self.lexicon = list()
        with open(data_path, encoding='utf-8') as f:
            for _line in f:
                answer_id, answer = _line.rstrip().split('\t')
                vocab_ids = answer.split(' ')
                self.answers[int(answer_id)] = vocab_ids
                self.lexicon.extend(vocab_ids)
        self.lexicon = collections.Counter(self.lexicon)


class QaData(object):

    def __init__(self, data_path):
        self.questions = list()
        self.positive = list()
        self.negative = list()
        self.lexicon = list()
        with open(data_path, encoding='utf-8') as f:
            for _line in f:
                values = _line.rstrip().split('\t')
                if len(values) == 2:
                    question, answer_ids = values
                    positive_ids = list(map(int, answer_ids.split(' ')))
                    negative_ids = list()
                elif len(values) == 3:
                    answer_ids, question, pool = values
                    positive_ids = list(map(int, answer_ids.split(' ')))
                    negative_ids = list(filter(lambda x: x not in set(positive_ids),
                                               map(int, pool.split(' '))))
                else:
                    continue
                vocab_ids = question.split(' ')
                self.questions.append(vocab_ids)
                self.lexicon.extend(vocab_ids)
                self.positive.append(positive_ids)
                self.negative.append(negative_ids)
        self.lexicon = collections.Counter(self.lexicon)


class InsuranceQaDataset(torch.utils.data.Dataset):

    def __init__(self, question_data, answer_data, vocab, max_length=200):
        self.vocab = vocab
        self.positive = question_data.positive
        self.negative = question_data.negative
        self.questions = list(map(self.vocab.word_ids, question_data.questions))
        self.answer_map = dict()
        for answer_id, vids in answer_data.answers.items():
            self.answer_map[answer_id] = self.vocab.word_ids(vids[:max_length])
        self.answers = list(self.answer_map.values())

    def __len__(self):
        return len(self.questions)

    def __getitem__(self, index):
        question, positive_ids, negative_ids = \
            self.questions[index], self.positive[index], self.negative[index]
        positive = self.answer_map[positive_ids[numpy.random.randint(len(positive_ids))]]
        if len(negative_ids) > 0:
            negative = self.answer_map[negative_ids[numpy.random.randint(len(negative_ids))]]
        else:
            negative = self.answers[numpy.random.randint(len(self.answers))]
        return torch.LongTensor(question), \
               torch.LongTensor(positive), \
               torch.LongTensor(negative)

    def get_qa_entry(self, index):
        question, positive_ids, negative_ids = \
            self.questions[index], self.positive[index], self.negative[index]
        positives = [self.answer_map[positive_id] for positive_id in positive_ids]
        negatives = [self.answer_map[negative_id] for negative_id in negative_ids]
        return question, positives, negatives

    @classmethod
    def collate(cls, batch):
        qs, ps, ns = zip(*batch)
        return torch.nn.utils.rnn.pad_sequence(qs, batch_first=True), \
               torch.nn.utils.rnn.pad_sequence(ps, batch_first=True), \
               torch.nn.utils.rnn.pad_sequence(ns, batch_first=True),

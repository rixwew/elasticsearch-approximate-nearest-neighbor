import torch
from torch.nn.utils.rnn import pack_padded_sequence, pad_packed_sequence


class SentenceEncoder(torch.nn.Module):

    def __init__(self, embedding_weights, hidden_size):
        super().__init__()
        embedding_weights = torch.FloatTensor(embedding_weights)
        self.embedding = torch.nn.Embedding.from_pretrained(embedding_weights)
        self.rnn = torch.nn.LSTM(embedding_weights.shape[-1], hidden_size,
                                 batch_first=True, bidirectional=True)

    def forward(self, x):
        lengths = (-x.data.eq(0).long() + 1).sum(1)
        _, idx_sort = torch.sort(lengths, dim=0, descending=True)
        _, idx_unsort = torch.sort(idx_sort, dim=0)
        x = x.index_select(0, idx_sort)
        lengths = lengths.index_select(0, idx_sort)
        x = self.embedding(x)
        x = pack_padded_sequence(x, lengths, batch_first=True)
        x, *_ = self.rnn(x)
        x, _ = pad_packed_sequence(x, batch_first=True, padding_value=float('-inf'))
        x, _ = torch.max(x, dim=1)
        norm = x.norm(p=2, dim=1, keepdim=True)
        x = x.div(norm)
        x = x.index_select(0, idx_unsort)
        return x

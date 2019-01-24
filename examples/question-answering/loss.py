import torch


class QaLoss(torch.nn.Module):

    def __init__(self, margin):
        super().__init__()
        self.margin = margin

    def forward(self, question, positive, negative):
        """
        max {0, margin - cosine(q, a+) + cosine(q, a-)}
        """
        positive_sim = (question * positive).sum(1, keepdim=True)
        negative_sim = (question * negative).sum(1, keepdim=True)
        zeros = positive_sim.data.new_zeros(*positive_sim.shape)
        loss = torch.cat((zeros, negative_sim - positive_sim + self.margin), dim=1)
        loss, _ = torch.max(loss, dim=1)
        return torch.mean(loss)

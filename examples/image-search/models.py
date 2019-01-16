import torch
import torchvision


class ImageEncoder(torch.nn.Module):

    def __init__(self):
        super(ImageEncoder, self).__init__()
        model = torchvision.models.alexnet(True)
        model.classifier = torch.nn.Sequential(
            *list(model.classifier.children())[:-1])
        self.encoder = model

    def forward(self, x):
        x = self.encoder(x)
        norm = x.norm(p=2, dim=1, keepdim=True)
        x = x.div(norm.expand_as(x))
        return x

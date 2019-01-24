# Question answering Example

Question answering implementation is based on paper LSTM-based Deep Learning Models
for Non-factoid Answer Selection - Tan, dos Santos, Xiang and Zhou.

## Requirement

* pytorch 1.0
* numpy
* gensim
* elasticsearch

## Download insurance qa data and train model

```bash
bash prepare.sh
python train.py
```

InsuranceQA Version1 top1 precision result

| Model                            | Validation | Test1 | Test2 |
|:---------------------------------|-----------:|------:|------:|
| QA-LSTM basic-model, max pooling(100 epoch) | 62.2 | 63.8 | 58.8 |
| QA-LSTM basic-model, max pooling(paper) | 64.3 | 63.1 | 58.0 |


## Search answers using elasticsearch plugin

```bash
export PYTHONPATH=$PATH_TO_SCRIPT_DIR/lib:$PYTHONPATH
python search_example.py --question "Can a Non us citizen get Life Insurance"
                         --result_size 5
```

```json
{
  "took": 36,
  "timed_out": false,
  "_shards": {
    "total": 5,
    "successful": 5,
    "skipped": 0,
    "failed": 0
  },
  "hits": {
    "total": 870,
    "max_score": null,
    "hits": [
      {
        "_index": "answers",
        "_type": "answer",
        "_id": "o1i1f2gBaJEWlukYG7sK",
        "_score": 0.5443098,
        "_source": {
          "description": "a non citizen can get life insurance with most company if they have a green card or an H-1b work visa some company do require the applicant be a US citizen before allow them get a life insurance policy and some will only allow green card but not work visa contact an agent find out which company will work for your situation"
        },
        "sort": [
          0.5443098
        ]
      },
      {
        "_index": "answers",
        "_type": "answer",
        "_id": "81i2f2gBaJEWlukYacAb",
        "_score": 0.7198508,
        "_source": {
          "description": "yes there be absolutely no requirement a person be a citizen buy life insurance each company make its own decision on requirement but citizenship be not 1 them so long as you be in the country legally you can buy life insurance different ID be require different carrier but rest assure if your age and health warrant it you can buy life insurance on yourself here in the USA love help thank you Gary Lane"
        },
        "sort": [
          0.7198508
        ]
      },
      {
        "_index": "answers",
        "_type": "answer",
        "_id": "0Fiyf2gBaJEWlukYdLDC",
        "_score": 0.75013983,
        "_source": {
          "description": "you do not have be a citizen obtain life insurance US life insurer require the propose insured must be a permanent resident of the US that mean a US citizen or a non US citizen who be a lawful permanent US resident ( green card or on certain visa type the applicant will also need have the means pay premium and have a demonstrable life insurance need i.e. generate earn income or asset protect here some insurer have develop foreign national program that can also work in situation where established US interest and tie exist plus meet some additional criterion citizen of some country may not be eligible it can be a complex area of field underwriting so much so that our firm have develop a special questionnaire help shop for coverage be sure work with a life insurance professional with experience in this area"
        },
        "sort": [
          0.75013983
        ]
      },
      {
        "_index": "answers",
        "_type": "answer",
        "_id": "9Fi2f2gBaJEWlukYacBr",
        "_score": 0.75358534,
        "_source": {
          "description": "yes a non US citizen can get life insurance with many American company it be up to the discretion of each company as to what type of citizenship or residency they will accept a green card be usually ok and many company will accept a work visa as qualification for apply for life insurance in the US get life insurance in the US as a non US citizen however almost always require have a residence in the United States"
        },
        "sort": [
          0.75358534
        ]
      },
      {
        "_index": "answers",
        "_type": "answer",
        "_id": "Nliwf2gBaJEWlukYIKcx",
        "_score": 0.7816857,
        "_source": {
          "description": "almost anyone can get life insurance the only people who can not get life insurance those who have serious health problem who fall outside the age guideline guarantee issue those who do not have any income at all even they may able to get a policy with a cap on the face amount in the us those who do not have citizenship a green card work visa"
        },
        "sort": [
          0.7816857
        ]
      }
    ]
  }
}
```

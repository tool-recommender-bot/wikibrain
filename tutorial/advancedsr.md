---
        layout: default
        title: Advanced SR usage.
---

# Advanced Semantic Relatedness Usage

WikiBrain provides a robust suite of SR measures and tools.
By default, when you load some language editions, Wikipedia trains the Milne Witten [2006] SR measure.
This algorithm strikes an excellent balance between accuracy, speed, and coverage.
However, you may tap into a variety of other algorithms.

## Modes of operation

As described in the SR overview page, WikiBrain supports a variety of modes of operation.
The two key dimensions it supports are:

 * Method:
   * `mostSimilar(A)`: finds the most similar concepts for a particular query `A`.
   * `similarity(A, B)`: estimates the similarity of concepts `A` and `B`.
   * `cosimilarity(rows, cols)`: builds a cosimilarity matrix between the concept in `rows` and `cols.
 * Query mode:
   * `phrase`: Concepts are represented by free-text queries.
   * `article`: Concepts are represented by Wikipedia article via their id.

SR researchers have traditionally studied the `similarity` / `phrase` cell in this six cell (two by three) grid.
However, WikiBrain suports all six cells.
In fact, in our experience applications are more likely to need `cosimilarity` and `mostSimilar` than `similarity`.

## Efficiencey

There are two dimensions of efficiency an application needs to consider.
**Training complexity** describes the amount of CPU time needed to create the data needed by an SR measure.
It is incurred once, offline, before an application is launched.
**Online complexity** describes the amount of CPU time needed to perform an API. 
This cost is incurred for every invocation of an SR API call.
There is often a tradeoff between online and training complexity.
Many algorithms that perform quickly and accurately reuqire longer training time.

One important example of the training vs online complexity tradeoff is in precomputing **feature matrices** for articles.
Many SR measures can precompute feature matrices for all articles in a language.
In doing so, a user pays a training penalty in exchange for runtime performance.
For SR measures that support them, feature matrices are built by default when a user runs the SRBuilder in mostSimilar mode (next section).

## Summary of Algorithmic Tradeoffs

The table below summarizes the most commonly used SR measures in WikiBrain and the tradeoffs between them.

| Metric | Data used | Accuracy | Training complexity | Runtime complexity | Has feature matrix |
|----|----|---|---|---|---|
| ESA | text | med-high | slow | slow | yes |
| ensemble | ensemble | high | slow | slow | no |
| milnewitten | links | med-high | fast | fast | yes |
| category | category | low | fast | fast | no |
| word2vec | text | med-high | high | low | yes |

## Running the SR builder

To use these algorithms, you must *build models* that capture the statistical relationships an SR metric uses to calculate similarities. To do this, run the SRBuilder java program for a particular SR metric (in this case the *ensemble* metric). You can also use your IDE or the "override command line" feature of the GUILoader to run the builder.

```bash
./wb-java.sh org.wikibrain.sr.SRBuilder -l simple -m ensemble -o both
```
The above example shows the three key arguments for running the SR builder.
The `-l` argument specifies the langauge of the SR metric you want to build.
The `-m` argument specifies the name of the metric (from the table above).
The `-o` argument specifies the mode. It can be `similarity`, `cosimilarity`, or `both`. If `both` is specified, feature matrices will be constructed if the SR measure supports them (see efficiency, above.)

## Normalizers

Each SR measure produces estiamted SR values on an arbitrary scale. 
To make the values more interpretable, WikiBrain offers a variety of normalizers that translate estimated values to a consistent scale.
Most SR measures use the `percentile` normalizer, which converts all values to percentiles on a 0 to 1.0 scale.
Many more normalizers are available in the [org.wikibrain.sr.normalize](https://github.com/shilad/wikibrain/tree/master/wikibrain-sr/src/main/java/org/wikibrain/sr/normalize) package. You can override a particular metric's normalizer by specifying something like the following in your configuration file:

```
sr.metric.local.milnewitten.similaritynormalizer : loess
sr.metric.local.milnewitten.mostsimilarnormalizer : loess
```

Be aware that you must rebuild your SR metric after changing the normalizer.
package org.deeplearning4j.recursive;

import org.apache.commons.io.FileUtils;
import org.deeplearning4j.models.rntn.RNTN;
import org.deeplearning4j.models.rntn.RNTNEval;
import org.deeplearning4j.models.word2vec.Word2Vec;
import org.deeplearning4j.nn.layers.feedforward.autoencoder.recursive.Tree;
import org.deeplearning4j.optimize.api.IterationListener;
import org.deeplearning4j.optimize.listeners.ScoreIterationListener;
import org.deeplearning4j.text.corpora.treeparser.TreeVectorizer;
import org.deeplearning4j.text.sentenceiterator.CollectionSentenceIterator;
import org.deeplearning4j.text.sentenceiterator.SentenceIterator;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;

import java.util.*;


/**
 * Recursive Neural Tensor Network (RNTN)
 *
 * Created by willow on 5/11/15.
 *
 */

public class RNTNTweetsExample2 {

    private static final Logger log = LoggerFactory.getLogger(RNTNTweetsExample2.class);

//    private static String fileName = "tweets_clean.txt";
    private static SentenceIterator sentenceIter;
    private static TreeVectorizer vectorizer;
    private static String fileName = "sentiment-tweets-small.csv";
    private static int batchSize = 1000;
    private static List<String> labels = new ArrayList<>();


    void loadData() throws Exception {
        List<String> sentences = new ArrayList<>();
        List<String> lines = FileUtils.readLines(new ClassPathResource(fileName).getFile());

        for (String line : lines) {
            String cols[] = line.split(",");
            labels.add(cols[1]);
            sentences.add(cols[3]);
        }
        sentenceIter = new CollectionSentenceIterator(sentences);
    }

    static Word2Vec buildVectors() throws Exception {
        Word2Vec featureVec = new Word2Vec.Builder()
                .batchSize(batchSize)
                .iterations(3)
                .sampling(1e-5)
                .minWordFrequency(5)
                .useAdaGrad(false)
                .layerSize(300)
                .learningRate(0.025)
                .minLearningRate(1e-2)
                .negativeSample(10)
                .iterate(sentenceIter)
                .build();
        featureVec.fit();
        return featureVec;
    }

    static RNTN buildModel(Word2Vec featureVec) {
        RNTN rntn = new RNTN.Builder()
                .setActivationFunction("tanh")
                .setAdagradResetFrequency(1)
                .setCombineClassification(true)
                .setFeatureVectors(featureVec)
                .setRandomFeatureVectors(false)
                .setUseTensors(false)
                .build();
        return rntn;
    }

    static RNTN trainModel(RNTN model) throws Exception {
        int count = 0;
        vectorizer = new TreeVectorizer();

        while(sentenceIter.hasNext()) {
            List<Tree> treeList = vectorizer.getTreesWithLabels(sentenceIter.nextSentence(), Arrays.asList(labels.get(count++)));
            model.fit(treeList);
        }
        return model;
    }

    static void evalModel(RNTN model) throws Exception {
        int count = 0;
        // Evaluate per node - each sentence is a parse tree
        RNTNEval eval = new RNTNEval();

        while(sentenceIter.hasNext());
            List<Tree> treeList = vectorizer.getTreesWithLabels(sentenceIter.nextSentence(), Arrays.asList(labels.get(count++)));
        eval.eval(model, treeList);
        log.info(eval.stats());
    }

    public void exec(String[] args) throws Exception {
        CmdLineParser parser = new CmdLineParser(this);
        try {
            parser.parseArgument(args);

        } catch (CmdLineException e) {
            // handling of wrong arguments
            System.err.println(e.getMessage());
            parser.printUsage(System.err);
        }

        log.info("Load & vectorize data....");
        loadData();
        Word2Vec featureVec = buildVectors();

        log.info("Build model....");
        RNTN model = buildModel(featureVec);

        log.info("Train model....");
        Collections.singletonList((IterationListener) new ScoreIterationListener(1));
        model = trainModel(model);

        log.info("Evaluate model....");
        sentenceIter.reset();
        evalModel(model);

        log.info("****************Example finished********************");

    }

    public static void main(String[] args) throws Exception {

        new RNTNTweetsExample2().exec(args);

    }

}

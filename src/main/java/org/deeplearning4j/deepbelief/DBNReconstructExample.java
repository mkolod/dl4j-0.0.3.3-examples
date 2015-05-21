package org.deeplearning4j.deepbelief;


import org.deeplearning4j.datasets.iterator.DataSetIterator;
import org.deeplearning4j.datasets.iterator.MultipleEpochsIterator;
import org.deeplearning4j.datasets.iterator.impl.MnistDataSetIterator;
import org.deeplearning4j.eval.Evaluation;
import org.deeplearning4j.nn.conf.MultiLayerConfiguration;
import org.deeplearning4j.nn.conf.NeuralNetConfiguration;
import org.deeplearning4j.nn.conf.layers.RBM;
import org.deeplearning4j.nn.conf.override.ClassifierOverride;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.deeplearning4j.nn.weights.WeightInit;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.dataset.DataSet;
import org.nd4j.linalg.lossfunctions.LossFunctions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Created by agibsonccc on 9/11/14.
 *
 * Diff from small adds Multiple Epochs Iterator and ClassifierOverride
 */
public class DBNReconstructExample {

    private static Logger log = LoggerFactory.getLogger(DBNReconstructExample.class);

    public static void main(String[] args) throws Exception {

        log.info("Load data....");
        DataSetIterator iter = new MultipleEpochsIterator(10, new MnistDataSetIterator(1000,1000));

        log.info("Build model....");
        MultiLayerConfiguration conf = new NeuralNetConfiguration.Builder()
                .layer(new RBM()).nIn(784).nOut(10).weightInit(WeightInit.VI).iterations(5)
                .lossFunction(LossFunctions.LossFunction.RMSE_XENT).learningRate(1e-1f)
                .list(4).hiddenLayerSizes(new int[]{600, 500, 400})
                .override(new ClassifierOverride(3))
                .build();
        MultiLayerNetwork model = new MultiLayerNetwork(conf);

        log.info("Train model....");
        model.fit(iter);
        iter.reset();

        log.info("Evaluate model....");
        Evaluation eval = new Evaluation();
        while(iter.hasNext()) {
            DataSet test_data = iter.next();
            INDArray predict2 = model.output(test_data.getFeatureMatrix());
            eval.eval(test_data.getLabels(), predict2);
        }

        log.info(eval.stats());
        log.info("****************Example finished********************");

    }

}

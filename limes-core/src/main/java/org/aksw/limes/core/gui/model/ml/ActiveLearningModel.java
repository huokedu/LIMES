package org.aksw.limes.core.gui.model.ml;

import javafx.concurrent.Task;

import org.aksw.limes.core.evaluation.qualititativeMeasures.PseudoFMeasure;
import org.aksw.limes.core.gui.model.Config;
import org.aksw.limes.core.io.cache.Cache;
import org.aksw.limes.core.io.mapping.AMapping;
import org.aksw.limes.core.io.mapping.MappingFactory;
import org.aksw.limes.core.ml.oldalgorithm.MLModel;

public class ActiveLearningModel extends MachineLearningModel {
    
    private AMapping nextExamples = MappingFactory.createDefaultMapping();
    public static final int nextExamplesNum = 3;

    public ActiveLearningModel(Config config, Cache sourceCache, Cache targetCache) {
        super(config, sourceCache, targetCache);
    }

    @Override
    public Task<Void> createLearningTask() {
        return new Task<Void>() {
            @Override
            protected Void call() {
        	MLModel model = null;
                try {
                    mlalgorithm.init(learningParameters, sourceCache, targetCache);
                    mlalgorithm.asActive().activeLearn();
                    nextExamples = mlalgorithm.asActive().getNextExamples(nextExamplesNum);
                } catch (Exception e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
                setLearnedMapping(mlalgorithm.predict(sourceCache, targetCache, model));
                return null;
            }
        };
    }

    public AMapping getNextExamples() {
        return nextExamples;
    }

    public void setNextExamples(AMapping nextExamples) {
        this.nextExamples = nextExamples;
    }


}

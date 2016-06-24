package org.aksw.limes.core.ml;

import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.aksw.limes.core.evaluation.qualititativeMeasures.PseudoFMeasure;
import org.aksw.limes.core.exceptions.UnsupportedMLImplementationException;
import org.aksw.limes.core.io.cache.Cache;
import org.aksw.limes.core.io.cache.Instance;
import org.aksw.limes.core.io.cache.MemoryCache;
import org.aksw.limes.core.io.config.Configuration;
import org.aksw.limes.core.io.config.KBInfo;
import org.aksw.limes.core.io.mapping.AMapping;
import org.aksw.limes.core.io.mapping.MappingFactory;
import org.aksw.limes.core.ml.algorithm.ActiveMLAlgorithm;
import org.aksw.limes.core.ml.algorithm.Eagle;
import org.aksw.limes.core.ml.algorithm.MLAlgorithmFactory;
import org.aksw.limes.core.ml.algorithm.MLImplementationType;
import org.aksw.limes.core.ml.algorithm.MLResults;
import org.aksw.limes.core.ml.algorithm.SupervisedMLAlgorithm;
import org.aksw.limes.core.ml.algorithm.UnsupervisedMLAlgorithm;
import org.aksw.limes.core.ml.algorithm.WombatSimple;
import org.aksw.limes.core.ml.algorithm.eagle.util.PropertyMapping;
import org.aksw.limes.core.ml.setting.LearningParameter;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Tommaso Soru <tsoru@informatik.uni-leipzig.de>
 *
 */
public class EagleTest {

    Cache sc = new MemoryCache();
    Cache tc = new MemoryCache();

    AMapping trainingMap, refMap;

    Configuration config = new Configuration();
    PropertyMapping pm = new PropertyMapping();
    
    @Before
    public void init() {
        List<String> props = new LinkedList<String>();
        props.add("name");
        props.add("surname");

        Instance i1 = new Instance("ex:i1");
        i1.addProperty("name", "Klaus");
        i1.addProperty("surname", "Lyko");
        Instance i2 = new Instance("ex:i2");
        i2.addProperty("name", "John");
        i2.addProperty("surname", "Doe");
        Instance i3 = new Instance("ex:i3");
        i3.addProperty("name", "Claus");
        i3.addProperty("surname", "Stadler");

        sc.addInstance(i1);
        sc.addInstance(i3);

        tc.addInstance(i1);
        tc.addInstance(i2);
        tc.addInstance(i3);

        trainingMap = MappingFactory.createDefaultMapping();
        trainingMap.add("ex:i1", "ex:i1", 1d);

        refMap = MappingFactory.createDefaultMapping();
        refMap.add("ex:i1", "ex:i1", 1d);
        refMap.add("ex:i3", "ex:i3", 1d);
        
        KBInfo si = new KBInfo();
        si.setVar("?x");
        si.setProperties(props);

        KBInfo ti = new KBInfo();
        ti.setVar("?y");
        ti.setProperties(props);

        config.setSourceInfo(si);
        config.setTargetInfo(ti);
        
        pm.addStringPropertyMatch("name", "name");
        pm.addStringPropertyMatch("surname", "surname");

    }

    @Test
    public void testSupervisedBatch() throws UnsupportedMLImplementationException {
        SupervisedMLAlgorithm eagleSup = null;
        try {
            eagleSup = MLAlgorithmFactory.createMLAlgorithm(Eagle.class,
                    MLImplementationType.SUPERVISED_BATCH).asSupervised();
        } catch (UnsupportedMLImplementationException e) {
            e.printStackTrace();
            fail();
        }
        assert (eagleSup.getClass().equals(SupervisedMLAlgorithm.class));
        eagleSup.init(null, sc, tc);
        eagleSup.getMl().setConfiguration(config);
        eagleSup.setParameter(Eagle.PROPERTY_MAPPING, pm);
        
        MLResults mlModel = eagleSup.learn(trainingMap);
        AMapping resultMap = eagleSup.predict(sc, tc, mlModel);
        assert (resultMap.equals(refMap));  
    }


//    @Test
//    public void testUnsupervised() throws UnsupportedMLImplementationException {
//        UnsupervisedMLAlgorithm wombatSimpleU = null;
//        try {
//            wombatSimpleU = MLAlgorithmFactory.createMLAlgorithm(WombatSimple.class,
//                    MLImplementationType.UNSUPERVISED).asUnsupervised();
//        } catch (UnsupportedMLImplementationException e) {
//            e.printStackTrace();
//            fail();
//        }
//        assert (wombatSimpleU.getClass().equals(UnsupervisedMLAlgorithm.class));
//        trainingMap = null;
//        wombatSimpleU.init(null, sc, tc);
//        MLResults mlModel = wombatSimpleU.learn(new PseudoFMeasure());
//        AMapping resultMap = wombatSimpleU.predict(sc, tc, mlModel);
//        assert (resultMap.equals(refMap));
//    }
//    
//    @Test
//    public void testActive() throws UnsupportedMLImplementationException {
//        ActiveMLAlgorithm wombatSimpleA = null;
//        try {
//            wombatSimpleA = MLAlgorithmFactory.createMLAlgorithm(WombatSimple.class,
//                    MLImplementationType.SUPERVISED_ACTIVE).asActive();
//        } catch (UnsupportedMLImplementationException e) {
//            e.printStackTrace();
//            fail();
//        }
//        assert (wombatSimpleA.getClass().equals(ActiveMLAlgorithm.class));
//        wombatSimpleA.init(null, sc, tc);
//        wombatSimpleA.activeLearn();
//        AMapping nextExamples = wombatSimpleA.getNextExamples(3);
//        AMapping oracleFeedback = oracleFeedback(nextExamples,trainingMap);
//        MLResults mlModel = wombatSimpleA.activeLearn(oracleFeedback);
//        AMapping resultMap = wombatSimpleA.predict(sc, tc, mlModel);
//        assert (resultMap.equals(refMap));
//    }
//    
//    private AMapping oracleFeedback(AMapping predictionMapping, AMapping referenceMapping) {
//        AMapping result = MappingFactory.createDefaultMapping();
//
//        for(String s : predictionMapping.getMap().keySet()){
//            for(String t : predictionMapping.getMap().get(s).keySet()){
//                if(referenceMapping.contains(s, t)){
//                    result.add(s, t, predictionMapping.getMap().get(s).get(t));
//                }
//            }
//        }
//        return result;
//    }

}
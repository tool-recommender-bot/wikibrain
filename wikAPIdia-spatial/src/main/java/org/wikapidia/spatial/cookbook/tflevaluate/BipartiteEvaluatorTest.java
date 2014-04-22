package org.wikapidia.spatial.cookbook.tflevaluate;

import com.google.common.collect.Sets;
import com.vividsolutions.jts.geom.Geometry;
import gnu.trove.procedure.TIntProcedure;
import gnu.trove.set.TIntSet;
import org.wikapidia.conf.Configurator;
import org.wikapidia.core.cmd.Env;
import org.wikapidia.core.cmd.EnvBuilder;
import org.wikapidia.core.dao.LocalPageDao;
import org.wikapidia.core.dao.UniversalPageDao;
import org.wikapidia.core.lang.Language;
import org.wikapidia.core.lang.LanguageSet;
import org.wikapidia.core.model.NameSpace;
import org.wikapidia.core.model.Title;
import org.wikapidia.core.model.UniversalPage;
import org.wikapidia.spatial.core.dao.SpatialContainmentDao;
import org.wikapidia.spatial.core.dao.SpatialDataDao;
import org.wikapidia.wikidata.WikidataDao;

import java.io.File;
import java.util.*;
import java.util.logging.Logger;

/**
 * Created by toby on 4/17/14.
 */
public class BipartiteEvaluatorTest {
    private static final Logger LOG = Logger.getLogger(BipartiteEvaluatorTest.class.getName());

    private static Set<Integer> PickSample(Set<Integer> originalSet, Integer size){
        if (size > originalSet.size()){
            LOG.warning(String.format("Want %d elements, only have %d", size, originalSet.size()));
            return originalSet;
        }
        List<Integer> list = new LinkedList<Integer>(originalSet);
        Collections.shuffle(list);
        return new HashSet<Integer>(list.subList(0, size));
    }

    public static void main(String[] args) throws Exception {

        Env env = EnvBuilder.envFromArgs(args);
        Configurator conf = env.getConfigurator();
        ToblersLawEvaluator evaluator = new ToblersLawEvaluator(env, new LanguageSet("simple"));
        SpatialDataDao sdDao = conf.get(SpatialDataDao.class);
        SpatialContainmentDao scDao = conf.get(SpatialContainmentDao.class);
        LocalPageDao lpDao = conf.get(LocalPageDao.class);
        WikidataDao wdDao = conf.get(WikidataDao.class);
        UniversalPageDao upDao = conf.get(UniversalPageDao.class);




        String layerName1 = "country";
        String layerName2 = "states";
        Set<String> subLayers = Sets.newHashSet();
        subLayers.add("wikidata");



        Integer containerId1 = wdDao.getItemId(lpDao.getByTitle(new Title("Germany", Language.getByLangCode("simple")), NameSpace.ARTICLE));
        TIntSet containedItemIds1 = scDao.getContainedItemIds(containerId1,layerName1, "earth", subLayers, SpatialContainmentDao.ContainmentOperationType.CONTAINMENT);

        Integer containerId2 = wdDao.getItemId(lpDao.getByTitle(new Title("New York", Language.getByLangCode("simple")), NameSpace.ARTICLE));
        TIntSet containedItemIds2 = scDao.getContainedItemIds(containerId2,layerName2, "earth", subLayers, SpatialContainmentDao.ContainmentOperationType.CONTAINMENT);

        Map<Integer, Geometry> geometriesToParse = new HashMap<Integer, Geometry>();
        List<UniversalPage> concepts1 = new ArrayList<UniversalPage>();
        List<UniversalPage> concepts2 = new ArrayList<UniversalPage>();

        final Set<Integer> containedId1 = new HashSet<Integer>();
        final Set<Integer> containedId2 = new HashSet<Integer>();


        LOG.info(String.format("%d items from set1, %d items from set2", containedItemIds1.size(), containedItemIds2.size()));
        int counter = 0;



        containedItemIds1.forEach(new TIntProcedure() {
            @Override
            public boolean execute(int i) {
                containedId1.add(i);
                return true;
            }
        });

        Set<Integer> sampledContainedId1 = PickSample(containedId1, 500);
        Set<Integer> sampledContainedId2 = PickSample(containedId2, 500);

        for(Integer i : sampledContainedId1){
            if(counter % 100 == 0)
                LOG.info(String.format("%d geometries added out of %d", counter, sampledContainedId1.size()));
            geometriesToParse.put(i, sdDao.getGeometry(i, "wikidata", "earth"));
            concepts1.add(upDao.getById(i, 1));
            counter ++;
        }

        containedItemIds2.forEach(new TIntProcedure() {
            @Override
            public boolean execute(int i) {
                containedId2.add(i);
                return true;
            }
        });



        counter = 0;
        for(Integer i : sampledContainedId2){
            if(counter % 100 == 0)
                LOG.info(String.format("%d geometries added out of %d", counter, sampledContainedId2.size()));
            geometriesToParse.put(i, sdDao.getGeometry(i, "wikidata", "earth"));
            concepts2.add(upDao.getById(i, 1));
            counter ++;
        }

        LOG.info(String.format("Now retrieving %d locations", geometriesToParse.size()));
        evaluator.retrieveLocations(geometriesToParse);

        evaluator.evaluateBipartite(new File("GERMANY-NY_Test.csv"), concepts1, concepts2);








    }




}
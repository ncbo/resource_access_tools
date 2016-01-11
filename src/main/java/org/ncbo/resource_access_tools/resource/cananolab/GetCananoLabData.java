package org.ncbo.resource_access_tools.resource.cananolab;

import edu.wustl.obr.QueryExecutor;
import edu.wustl.utill.CaNanoLabNodeDetail;
import edu.wustl.utill.ContextInvoker;
import gov.nih.nci.cananolab.domain.particle.Sample;
import org.apache.log4j.Logger;
import org.ncbo.resource_access_tools.populate.Element;
import org.ncbo.resource_access_tools.populate.Element.BadElementStructureException;
import org.ncbo.resource_access_tools.populate.Structure;
import org.ncbo.resource_access_tools.resource.ResourceAccessTool;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

/**
 * This class main task is to fetch contextKeys values from context and create a
 * element.
 *
 * @author lalit_chand
 */

class GetCananoLabData {

    private static final Logger logger = Logger.getLogger(GetCananoLabData.class);


    private int nbElement;

    /**
     * @return It returns all cananoLab particles
     */
    public List<CaNanoLabNodeDetail> getCananolabNodeDetails() {
        return new QueryExecutor(null).getCananolabNodeDetails();
    }

    /**
     * @param details
     * @param resource
     * @return
     */
    public int insertElements(List<CaNanoLabNodeDetail> details, ResourceAccessTool resource) {

        HashSet<String> allElementLocalIDs = resource.getResourceUpdateService().getAllLocalElementIDs();
        int MAX_THREADS = 5;
        List<CaNanoLabThread> caNanoLabThreads = new ArrayList<CaNanoLabThread>(MAX_THREADS);
        for (CaNanoLabNodeDetail detail : details) {
            logger.info(" Number of Nano Particle:" + detail.getSampleSet().size());

            for (Sample sp : detail.getSampleSet()) {

                if (allElementLocalIDs.contains(sp.getId().toString())) {
                    continue;
                }
                caNanoLabThreads.add(new CaNanoLabThread(resource, detail, sp));

                if (caNanoLabThreads.size() == MAX_THREADS) {
                    getDataFromCaNanoLabThreads(caNanoLabThreads);
                    caNanoLabThreads.clear();
                }
            }

            if (caNanoLabThreads.size() > 0) {
                getDataFromCaNanoLabThreads(caNanoLabThreads);
                caNanoLabThreads.clear();
            }
        }
        return nbElement;
    }

    /**
     * Get caNanlab element from caNanoLabThreads
     *
     * @param caNanoLabThreads
     */
    private void getDataFromCaNanoLabThreads(List<CaNanoLabThread> caNanoLabThreads) {

        for (CaNanoLabThread caNanoLabThread : caNanoLabThreads) {
            caNanoLabThread.start();
            try {
                Thread.sleep(10000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        // wait for all thead complete
        for (CaNanoLabThread caNanoLabThread : caNanoLabThreads) {
            while (caNanoLabThread.isAlive()) {
                try {
                    Thread.sleep(10000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }

    }

    /**
     * Class used for getting getting element from service and
     * populating it into element table.
     */
    private class CaNanoLabThread extends Thread {

        private final ResourceAccessTool resource;
        private final Sample sample;
        private final CaNanoLabNodeDetail detail;
        private final ContextInvoker caNanoContextInvoker;
        private final Structure elementStructure;


        public CaNanoLabThread(ResourceAccessTool resource, CaNanoLabNodeDetail detail, Sample sample) {
            super();
            this.resource = resource;
            this.detail = detail;
            this.sample = sample;
            this.caNanoContextInvoker = new ContextInvoker(sample.getId());
            this.elementStructure = resource.getToolResource().getResourceStructure();
        }

        @Override
        public void run() {
            Map<Double, String> nanoData = caNanoContextInvoker.getContextValue(detail.getServiceURL());
            String val = null;

            for (String contextName : elementStructure.getContextNames()) {
                // default ontology is given to all context ..
                if (elementStructure.getOntoID(contextName).equals(Structure.FOR_CONCEPT_RECOGNITION)) {
                    Double weight = elementStructure.getWeight(contextName);
                    val = nanoData.get(weight);
                    logger.debug(" ContextName:" + contextName + "  Weight:" + weight + "  Val:" + val);
                    if (val == null) {
                        val = "";
                    }

                    if (contextName.equals("CANANO_Sample")) {
                        val = val + " " + sample.getName();
                    }
                }

                if (elementStructure.getOntoID(contextName).equals(Structure.NOT_FOR_ANNOTATION)) {
                    if (contextName.equals("CANANO_url")) {
                        val = detail.getElementIdURL() + sample.getId() + detail.getLocalValue();
                    }
                }
                elementStructure.putContext(contextName, val);
            }

            Element caNanoElement = null;
            // put the elementStructure in a new element
            try {
                caNanoElement = new Element(sample.getId().toString(), elementStructure);
            } catch (BadElementStructureException e) {
                logger.error("Bad Element Structure Exception Occured .." + e);
            }

            if (caNanoElement != null && resource.getResourceUpdateService().addElement(caNanoElement)) {
                nbElement++;
            }
        }
    }
}

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.ncbo.resource_access_tools.resource.caarray;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import org.ncbo.resource_access_tools.resource.ResourceAccessTool;
import obs.obr.populate.Element;
import obs.obr.populate.Structure;
import org.ncbo.stanford.obr.enumeration.ResourceType;

/**
 * AccessTool for the resource CaArray.
 * @author s.kharat
 */
public class CaArrayAccessTool extends ResourceAccessTool {

    private static final String CARY_URL = "https://array.nci.nih.gov/caarray/home.action";
    private static final String CARY_NAME = "caArray";
    private static final String CARY_RESOURCEID = "CARY";
    private static final String CARY_DESCRIPTION = "caArray is an open-source, web and programmatically accessible array data management system. caArray guides the annotation and exchange of array data using a federated model of local installations whose results are shareable across the cancer Biomedical Informatics Grid (caBIG™). caArray furthers translational cancer research through acquisition, dissemination and aggregation of semantically interoperable array data to support subsequent analysis by tools and services on and off the Grid. As array technology advances and matures, caArray will extend its logical library of assay management.";
    private static final String CARY_LOGO = "https://array.nci.nih.gov/caarray/images/logo_caarray.gif";
    private static final String CARY_ELT_URL = "https://array-stage.nci.nih.gov/caarray/project/";
    private static final String[] CARY_ITEMKEYS = {"Title", "Description", "Organism", "Tissue_Sites", "Cell_Types", "Disease_State"};
    private static final Double[] CARY_WEIGHTS = {1.0, 0.8, 0.9, 0.8, 0.8, 0.8};
    private static final String[] CARY_ONTOIDS = {Structure.FOR_CONCEPT_RECOGNITION, Structure.FOR_CONCEPT_RECOGNITION, Structure.FOR_CONCEPT_RECOGNITION, Structure.FOR_CONCEPT_RECOGNITION, Structure.FOR_CONCEPT_RECOGNITION, Structure.FOR_CONCEPT_RECOGNITION};
    private static Structure CARY_STRUCTURE = new Structure(CARY_ITEMKEYS, CARY_RESOURCEID, CARY_WEIGHTS, CARY_ONTOIDS);
    private static String CARY_MAIN_ITEMKEY = "Title";
    private Map<String, String> localOntologyIDMap;

    // constructor
    public CaArrayAccessTool() {
        super(CARY_NAME, CARY_RESOURCEID, CARY_STRUCTURE);
        try {
            this.getToolResource().setResourceURL(new URL(CARY_URL));
            this.getToolResource().setResourceDescription(CARY_DESCRIPTION);
            this.getToolResource().setResourceLogo(new URL(CARY_LOGO));
            this.getToolResource().setResourceElementURL(CARY_ELT_URL);
        } catch (MalformedURLException e) {
            logger.error(EMPTY_STRING, e);
        }
        localOntologyIDMap = createLocalOntologyIDMap(CARY_STRUCTURE);
    }

    @Override
    public ResourceType getResourceType() {
        return ResourceType.SMALL;
    }

    @Override
    public void updateResourceInformation() {
        // TODO
        // can be used to update resource name, description, logo, elt_url.
    }

    @Override
    public HashSet<String> queryOnlineResource(String query) {
        // TODO
        // not used for caArray
        return new HashSet<String>();
    }

    @Override
    public String elementURLString(String elementLocalID) {
        return CARY_ELT_URL + elementLocalID;
    }

    @Override
    public String mainContextDescriptor() {
        return CARY_MAIN_ITEMKEY;
    }

    @Override
    public int updateResourceContent() {
        int nbElement = 0;
        try {
            Element myExp;

            //Get all elements from resource site
            HashSet<Element> annotList = this.getAllExperiments();
            logger.info("Number of new elements to dump: " + annotList.size());

            // for each experiments accessed by the tool
            Iterator<Element> i = annotList.iterator();
            while (i.hasNext()) {
                // populates OBR_CARY_ET with each of these experiments.
                myExp = i.next();
                try {
                    if (!myExp.getElementStructure().hasNullValues()) {
                        if (this.resourceUpdateService.addElement(myExp)) {
                            nbElement++;
                        }
                    }
                } catch (Exception e) {
                    logger.error("** PROBLEM ** Problem with disease " + myExp.getLocalElementId() + " when populating the OBR_CARY_ET table.", e);
                }
            }
        } catch (Exception e) {
            logger.error("** PROBLEM ** Cannot update resource " + this.getToolResource().getResourceName() , e);
        }
        logger.info(nbElement + " experiments added to the OBR_CARY_ET table.");
        return nbElement;
    }

    /**
     * get all Experiments by using service API.
     */
    public HashSet<Element> getAllExperiments() {
        logger.info("* Get All Experiments for caArray ... ");
        HashSet<Element> elementList = new HashSet<Element>();
        int nbAdded = 0;
        try {
            GetCaArrayData myExtractor = new GetCaArrayData(this.getToolResource());
            // Gets the elements already in the corresponding _ET
            HashSet<String> allElementsInET = this.resourceUpdateService.getAllLocalElementIDs();
            elementList = myExtractor.getElements(localOntologyIDMap, allElementsInET);
        } catch (Exception e) {
            logger.error("** PROBLEM ** Problem in getting Experiments.", e);
        }
        nbAdded = elementList.size();
        logger.info((nbAdded) + " Experiments found.");
        return elementList;
    }

    /**
     * This method creates map of latest version of ontology with contexts as key.
     * It uses virtual ontology ids associated with contexts.
     *
     * @param structure {@code Structure} for given resource
     * @return {@code HashMap} of latest local ontology id with context as key.
     */
    public HashMap<String, String> createLocalOntologyIDMap(Structure structure) {
        HashMap<String, String> localOntologyIDMap = new HashMap<String, String>();
        String virtualOntologyID;
        for (String contextName : structure.getOntoIds().keySet()) {
            virtualOntologyID = structure.getOntoIds().get(contextName);
            if (!virtualOntologyID.equals(Structure.FOR_CONCEPT_RECOGNITION)
                    && !virtualOntologyID.equals(Structure.NOT_FOR_ANNOTATION)) {
                localOntologyIDMap.put(contextName, ontlogyService.getLatestLocalOntologyID(virtualOntologyID));
            }
        }
        return localOntologyIDMap;
    }
}

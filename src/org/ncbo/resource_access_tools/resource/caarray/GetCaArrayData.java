/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.ncbo.resource_access_tools.resource.caarray;

import java.util.HashSet;
import java.util.Map;
import org.apache.log4j.Logger;
import obs.obr.populate.Element;
import obs.obr.populate.Resource;
import obs.obr.populate.Structure;
import obs.obr.populate.Element.BadElementStructureException;
import org.ncbo.stanford.obr.util.helper.StringHelper;
import gov.nih.nci.caarray.external.v1_0.experiment.Experiment;
import gov.nih.nci.caarray.external.v1_0.query.BiomaterialSearchCriteria;
import gov.nih.nci.caarray.external.v1_0.query.KeywordSearchCriteria;
import gov.nih.nci.caarray.external.v1_0.sample.Biomaterial;
import gov.nih.nci.caarray.external.v1_0.sample.BiomaterialType;
import gov.nih.nci.caarray.services.external.v1_0.CaArrayServer;
import gov.nih.nci.caarray.services.external.v1_0.search.JavaSearchApiUtils;
import gov.nih.nci.caarray.services.external.v1_0.search.SearchApiUtils;
import gov.nih.nci.caarray.services.external.v1_0.search.SearchService;
import java.util.HashMap;
import java.util.List;

/**
 * This class enables to get all CaArray resource experiments data by using service API.
 * @author s.kharat
 */
public class GetCaArrayData implements StringHelper {

    // Logger for this class
    private static Logger logger = Logger.getLogger(GetCaArrayData.class);
    private static SearchService searchService = null;
    private static SearchApiUtils searchServiceHelper = null;
    private static final String KEYPHRASE = "";// blank mins retrive all experiments
    // Connection properties
    private static final String SERVER_HOSTNAME_KEY = "server.hostname";
    private static final String SERVER_JNDI_PORT_KEY = "server.jndi.port";
    private static final String SERVER_HOSTNAME_DEFAULT = "array.nci.nih.gov";//array-stage.nci.nih.gov
    private static final String SERVER_JNDI_PORT_DEFAULT = "8080";
    Resource resource;
    Structure basicStructure = null;
    String resourceID = EMPTY_STRING;

    //constructor
    public GetCaArrayData(Resource myResource) {
        this.resource = myResource;
        this.basicStructure = myResource.getResourceStructure();
        this.resourceID = myResource.getResourceId();
    }

    public HashSet<Element> getElements(Map<String, String> localOntologyMap, HashSet<String> allElementsInET) {

        HashSet<Element> elementSet = new HashSet<Element>();
        Map<String, Map<String, String>> allExperiments = new HashMap<String, Map<String, String>>();

        try {
            CaArrayServer server = new CaArrayServer(this.getServerHostname(), this.getServerJndiPort());
            server.connect();
            searchService = server.getSearchService();
            searchServiceHelper = new JavaSearchApiUtils(searchService);

            KeywordSearchCriteria keycriteria = new KeywordSearchCriteria();
            keycriteria.setKeyword(KEYPHRASE);
            long startTime = System.currentTimeMillis();
            List<Experiment> experiments = (searchServiceHelper.experimentsByKeyword(keycriteria)).list();
            long totalTime = System.currentTimeMillis() - startTime;

            System.out.println("Found " + experiments.size() + " experiments in " + totalTime + " ms.");

            for (Experiment experiment : experiments) {

                Map<String, String> expAttribute = new HashMap<String, String>();
                StringBuffer diseaseState = new StringBuffer();
                StringBuffer tissueSites = new StringBuffer();
                StringBuffer materialTypes = new StringBuffer();
                StringBuffer cellTypes = new StringBuffer();

                String localElementID = experiment.getPublicIdentifier();

                //If element already present in ET then avoid parsing and element creation.
                if (allElementsInET.contains(localElementID)) {
                    continue;
                }

                String title = experiment.getTitle();
                String description = "";

                if (experiment.getDescription() != null) {
                    description = experiment.getDescription();
                }

                String organism = experiment.getOrganism().getScientificName();

                BiomaterialSearchCriteria criteria = new BiomaterialSearchCriteria();
                criteria.setExperiment(experiment.getReference());
                criteria.getTypes().add(BiomaterialType.SOURCE);
                List<Biomaterial> biomaterials = (searchService.searchForBiomaterials(criteria, null)).getResults();


                for (Biomaterial b : biomaterials) {
                    if (b.getDiseaseState() != null && b.getDiseaseState().getTerm() != null && !diseaseState.toString().contains(b.getDiseaseState().getTerm().getValue())) {
                        if (diseaseState.length() > 0) {
                            diseaseState.append(",");
                        }
                        diseaseState.append(b.getDiseaseState().getTerm().getValue());
                    }

                    if (b.getTissueSite() != null && b.getTissueSite().getTerm() != null && !tissueSites.toString().contains(b.getTissueSite().getTerm().getValue())) {
                        if (tissueSites.length() > 0) {
                            tissueSites.append(",");
                        }
                        tissueSites.append(b.getTissueSite().getTerm().getValue());
                    }
                    if (b.getCellType() != null && b.getCellType().getTerm() != null && !cellTypes.toString().contains(b.getCellType().getTerm().getValue())) {
                        if (cellTypes.length() > 0) {
                            cellTypes.append(",");
                        }
                        cellTypes.append(b.getCellType().getTerm().getValue());
                    }
                }

                expAttribute.put("Title", title);
                expAttribute.put("Description", description);
                expAttribute.put("Organism", organism);
                expAttribute.put("Disease_State", diseaseState.toString());
                expAttribute.put("Tissue_Sites", tissueSites.toString());
                expAttribute.put("Material_Types", materialTypes.toString());
                expAttribute.put("Cell_Types", cellTypes.toString());
                allExperiments.put(localElementID, expAttribute);
            }
            // end of parsing

            // Second phase: creation of elements
            for (String localElementID : allExperiments.keySet()) {
                Map<String, String> elementAttributes = new HashMap<String, String>();
                elementAttributes = allExperiments.get(localElementID);

                // PUT DATA INTO A STRUCTURE++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
                Structure elementStructure = new Structure(this.basicStructure.getContextNames());
                for (String contextName : this.basicStructure.getContextNames()) {
                    boolean attributeHasValue = false;

                    for (String att : elementAttributes.keySet()) {
                        if (contextName.equals(this.resourceID + UNDERSCORE_STRING + att)) {
                            // not an existing annotation
                            if (basicStructure.getOntoID(contextName).equals(Structure.FOR_CONCEPT_RECOGNITION)
                                    || basicStructure.getOntoID(contextName).equals(Structure.NOT_FOR_ANNOTATION)) {
                                elementStructure.putContext(contextName, elementAttributes.get(att));
                                attributeHasValue = true;

                            }
                        }
                    }

                    // to avoid null value in the structure
                    if (!attributeHasValue) {
                        elementStructure.putContext(contextName, EMPTY_STRING);
                    }
                }
                // put the element structure in a new element
                try {
                    Element exp = new Element(localElementID, elementStructure);
                    elementSet.add(exp);
                } catch (BadElementStructureException e) {
                    logger.error(EMPTY_STRING, e);
                }
            }
        } catch (Exception ioe) {
            logger.error(EMPTY_STRING, ioe);
        }

        return elementSet;
    }

    // Getters for Connection properties
    public static String getServerHostname() {
        return System.getProperty(SERVER_HOSTNAME_KEY, SERVER_HOSTNAME_DEFAULT);
    }

    public static int getServerJndiPort() {
        return Integer.parseInt(System.getProperty(SERVER_JNDI_PORT_KEY, SERVER_JNDI_PORT_DEFAULT));
    }
}

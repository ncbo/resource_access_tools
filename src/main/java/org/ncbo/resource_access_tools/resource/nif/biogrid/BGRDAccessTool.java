/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.ncbo.resource_access_tools.resource.nif.biogrid;

import org.jsoup.Jsoup;
import org.ncbo.resource_access_tools.enumeration.ResourceType;
import org.ncbo.resource_access_tools.populate.Element;
import org.ncbo.resource_access_tools.populate.Structure;
import org.ncbo.resource_access_tools.resource.nif.AbstractNifResourceAccessTool;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;

/**
 * AccessTool for BioGrid (via NIF).
 *
 * @author s.kharat
 */
public class BGRDAccessTool extends AbstractNifResourceAccessTool {

    private static final String URL = "http://thebiogrid.org/";
    private static final String NAME = "BioGRID (via NIF)";
    private static final String RESOURCEID = "BGRD";
    private static final String DESCRIPTION = "An online interaction repository of raw protein and genetic interactions from major model organism species, "
            + "with data compiled through comprehensive curation efforts. The current version searches over 36,000 articles containing over 560,000 interactions. "
            + "Complete coverage of the entire literature is maintained for budding yeast (S. cerevisiae), fission yeast (S. pombe) and thale cress (A. thaliana), "
            + "and efforts to expand curation across multiple metazoan species are underway.";
    private static final String LOGO = "http://neurolex.org/w/images/2/27/BioGRID.PNG";
    private static final String ELT_URL = "http://thebiogrid.org/search.php?search=";
    private static final String[] ITEMKEYS = {"Interactor_A_and_Interactor_B", "Interactor_A", "Organism_A", "Interactor_B", "Organism_B", "Interaction_Detection_Method", "Interaction_Types", "Source_Database", "Confidence_Values"};
    private static final Double[] WEIGHTS = {0.0, 1.0, 0.9, 0.9, 0.9, 0.5, 0.5, 0.0, 0.0};
    private static final String[] ONTOIDS = {Structure.NOT_FOR_ANNOTATION, Structure.FOR_CONCEPT_RECOGNITION, Structure.FOR_CONCEPT_RECOGNITION, Structure.FOR_CONCEPT_RECOGNITION,
            Structure.FOR_CONCEPT_RECOGNITION, Structure.FOR_CONCEPT_RECOGNITION, Structure.FOR_CONCEPT_RECOGNITION, Structure.NOT_FOR_ANNOTATION, Structure.NOT_FOR_ANNOTATION};
    private static final Structure STRUCTURE = new Structure(ITEMKEYS, RESOURCEID, WEIGHTS, ONTOIDS);
    // Constant
    private static final String nifId = "nif-0000-00432-1";
    private static final String InteractorA = "Interactor A";
    private static final String OrganismA = "Organism A";
    private static final String InteractorB = "Interactor B";
    private static final String OrganismB = "Organism B";
    private static final String Interaction_Detection_Method = "Interaction Detection Method";
    private static final String Interaction_Types = "Interaction Type";
    private static final String Source_Database = "Source Database";
    private static final String Confidence_Values = "Confidence Values";
    //Resource uniqueness is checked against four fields (InteractorA, InteractorB,Interaction_Detection_Method, and Interaction_Types)
    private static final String Unique_field_columns = "concat(" + Structure.generateContextName(RESOURCEID, ITEMKEYS[1]) + ","
            + Structure.generateContextName(RESOURCEID, ITEMKEYS[3]) + ","
            + Structure.generateContextName(RESOURCEID, ITEMKEYS[5]) + ","
            + Structure.generateContextName(RESOURCEID, ITEMKEYS[6]) + ")";

    // constructors
    public BGRDAccessTool() {
        super(NAME, RESOURCEID, STRUCTURE);
        try {
            this.getToolResource().setResourceURL(new URL(URL));
            this.getToolResource().setResourceDescription(DESCRIPTION);
            this.getToolResource().setResourceLogo(new URL(LOGO));
            this.getToolResource().setResourceElementURL(ELT_URL);
        } catch (MalformedURLException e) {
            logger.error(EMPTY_STRING, e);
        }
        Map<String, String> localOntologyIDMap = createLocalOntologyIDMap(STRUCTURE);
    }

    @Override
    public ResourceType getResourceType() {
        return ResourceType.BIG;
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
        //separating row count value from localElementId
        String elemetId[] = elementLocalID.split(SLASH_STRING);
        return ELT_URL + elemetId[0];
    }

    @Override
    public String mainContextDescriptor() {
        String MAIN_ITEMKEY = "Interactor_A_and_Interactor_B";
        return MAIN_ITEMKEY;
    }

    /**
     * This method creates map of latest version of ontology with contexts as key.
     * It uses virtual ontology ids associated with contexts.
     *
     * @param structure {@code Structure} for given resource
     * @return {@code HashMap} of latest local ontology id with context as key.
     */
    private HashMap<String, String> createLocalOntologyIDMap(Structure structure) {
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

    @Override
    public int updateResourceContent() {
        int nbElement = 0;
        try {
            Element myExp;
            //Get all elements from resource site
            HashSet<Element> allElementList = this.getAllElements();
            logger.info("Number of new elements to dump: " + allElementList.size());

            // for each experiments accessed by the tool
            Iterator<Element> i = allElementList.iterator();
            while (i.hasNext()) {
                myExp = i.next();
                try {
                    if (!myExp.getElementStructure().hasNullValues()) {
                        if (this.resourceUpdateService.addElement(myExp)) {
                            nbElement++;
                        }
                    }
                } catch (Exception e) {
                    logger.error("** PROBLEM ** Problem with id " + myExp.getLocalElementId() + " when populating the OBR_BGRD_ET table.", e);
                }
            }
        } catch (Exception e) {
            logger.error("** PROBLEM ** Cannot update resource " + this.getToolResource().getResourceName(), e);
        }
        logger.info(nbElement + " elements added to the OBR_BGRD_ET table.");
        return nbElement;
    }

    /**
     * This method is used to get all elements from resource site.
     *
     * @return HashSet<Element>
     */
    private HashSet<Element> getAllElements() {
        logger.info("* Get All Elements for NIF BioGRID ... ");
        HashSet<Element> elementSet = new HashSet<Element>();
        int nbAdded = 0;
        int offset = 0;
        int totalCount = 0;

        try {
            //get all elements from _ET table
            //Unique entry combination for this resource is checked against 4 fields (bgrd_interactor_a, bgrd_interactor_b, bgrd_interaction_detection_method, bgrd_interaction_types)
            HashSet<String> allElementsInET = this.resourceUpdateService.getAllValuesByColumn(Unique_field_columns);

            Map<String, Map<String, String>> allRowsData = new HashMap<String, Map<String, String>>();
            int rowcnt = 1;

            //parsing data
            do {
                Document dom = queryFederation(nifId, query, offset);
                if (dom != null) {
                    Node tableData = dom.getFirstChild().getChildNodes().item(1);
                    //get total records
                    totalCount = Integer.parseInt(tableData.getAttributes().getNamedItem(resultCount).getNodeValue());
                    offset += rowCount;

                    Node results = tableData.getChildNodes().item(1);

                    // Iterate over the returned structure
                    NodeList rows = results.getChildNodes();
                    for (int i = 0; i < rows.getLength(); i++) {
                        String localElementId = EMPTY_STRING;
                        String intA = EMPTY_STRING;
                        String intB = EMPTY_STRING;
                        String intDetMethod = EMPTY_STRING;
                        String intType = EMPTY_STRING;
                        Map<String, String> elementAttributes = new HashMap<String, String>();

                        Node row = rows.item(i);
                        for (int j = 0; j < row.getChildNodes().getLength(); j++) {
                            NodeList vals = row.getChildNodes().item(j).getChildNodes();
                            String name = null;
                            String value = null;
                            for (int k = 0; k < vals.getLength(); k++) {
                                if (nodeName.equals(vals.item(k).getNodeName())) {
                                    name = vals.item(k).getTextContent();
                                } else if (nodeValue.equals(vals.item(k).getNodeName())) {
                                    value = vals.item(k).getTextContent();
                                }
                            }
                            if (name.equalsIgnoreCase(InteractorA)) {
                                localElementId = value.substring(value.indexOf(ELT_URL) + ELT_URL.length(), value.indexOf(endTag));
                                intA = Jsoup.parse(value).text();
                                elementAttributes.put(Structure.generateContextName(RESOURCEID, ITEMKEYS[1]), intA);
                            } else if (name.equalsIgnoreCase(OrganismA)) {
                                elementAttributes.put(Structure.generateContextName(RESOURCEID, ITEMKEYS[2]), Jsoup.parse(value).text());
                            } else if (name.equalsIgnoreCase(InteractorB)) {
                                intB = Jsoup.parse(value).text();
                                elementAttributes.put(Structure.generateContextName(RESOURCEID, ITEMKEYS[3]), intB);
                            } else if (name.equalsIgnoreCase(OrganismB)) {
                                elementAttributes.put(Structure.generateContextName(RESOURCEID, ITEMKEYS[4]), Jsoup.parse(value).text());
                            } else if (name.equalsIgnoreCase(Interaction_Detection_Method)) {
                                intDetMethod = value;
                                elementAttributes.put(Structure.generateContextName(RESOURCEID, ITEMKEYS[5]), intDetMethod);
                            } else if (name.equalsIgnoreCase(Interaction_Types)) {
                                intType = value;
                                elementAttributes.put(Structure.generateContextName(RESOURCEID, ITEMKEYS[6]), intType);
                            } else if (name.equalsIgnoreCase(Source_Database)) {
                                elementAttributes.put(Structure.generateContextName(RESOURCEID, ITEMKEYS[7]), value);
                            } else if (name.equalsIgnoreCase(Confidence_Values)) {
                                elementAttributes.put(Structure.generateContextName(RESOURCEID, ITEMKEYS[8]), value);
                            }
                        }
                        elementAttributes.put(Structure.generateContextName(RESOURCEID, ITEMKEYS[0]), intA + "-" + intB);
                        //Check if elementId is present in database.
                        if (allElementsInET.contains(intA + intB + intDetMethod + intType)) {
                        } else {
                            allElementsInET.add(intA + intB + intDetMethod + intType);
//                          additional row count value appended to localElementId to overcome unique constraint restriction for this column in DB.
                            localElementId += SLASH_STRING + rowcnt;
                            rowcnt++;
                            allRowsData.put(localElementId, elementAttributes);
                        }

                    }
                } else {
                    offset += rowCount;
                }
            } while (offset < totalCount);

            //parsing ends

            // Second phase: creation of elements
            for (String localElementID : allRowsData.keySet()) {
                Map<String, String> elementAttributes = new HashMap<String, String>();
                elementAttributes = allRowsData.get(localElementID);

                // PUT DATA INTO A STRUCTURE++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
                Structure elementStructure = new Structure(STRUCTURE.getContextNames());
                for (String contextName : STRUCTURE.getContextNames()) {
                    boolean attributeHasValue = false;

                    for (String att : elementAttributes.keySet()) {
                        if (contextName.equals(att)) {
                            // not an existing annotation
                            if (STRUCTURE.getOntoID(contextName).equals(Structure.FOR_CONCEPT_RECOGNITION)
                                    || STRUCTURE.getOntoID(contextName).equals(Structure.NOT_FOR_ANNOTATION)) {
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
                    // Removing additional int value.
                    // localElementID = localElementID.substring(0,localElementID.indexOf(SLASH_STRING));
                    Element exp = new Element(localElementID, elementStructure);
                    elementSet.add(exp);
                } catch (Element.BadElementStructureException e) {
                    logger.error(EMPTY_STRING, e);
                }
            }

        } catch (Exception e) {
            logger.error("** PROBLEM ** Problem in getting rows.", e);
        }
        nbAdded = elementSet.size();
        logger.info((nbAdded) + " rows found.");
        return elementSet;
    }
}

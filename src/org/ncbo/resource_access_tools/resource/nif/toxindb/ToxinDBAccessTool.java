/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.ncbo.resource_access_tools.resource.nif.toxindb;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;

import org.jsoup.Jsoup;
import org.ncbo.resource_access_tools.enumeration.ResourceType;
import org.ncbo.resource_access_tools.populate.Element;
import org.ncbo.resource_access_tools.populate.Structure;
import org.ncbo.resource_access_tools.resource.nif.AbstractNifResourceAccessTool;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * AccessTool for NIF ToxinDB
 * @author s.kharat
 */
public class ToxinDBAccessTool extends AbstractNifResourceAccessTool {

    private static final String TOXIN_URL = "http://www.t3db.org/";
    private static final String TOXIN_NAME = "ToxinDB (via NIF)";
    private static final String TOXIN_RESOURCEID = "TOXIN";
    private static final String TOXIN_DESCRIPTION = "Toxin and Toxin Target Database (T3DB) is a unique bioinformatics resource that combines detailed toxin data with comprehensive toxin target information. The database currently houses over 2900 toxins described by over 34200 synonyms, including pollutants, pesticides, drugs, and food toxins, which are linked to over 1300 corresponding toxin target records. Altogether there are over 33800 toxins, toxin target associations. "
            + "Each toxin record (ToxCard) contains over 50 data fields and holds information such as chemical properties and descriptors, toxicity values, molecular and cellular interactions, and medical information. This information has been extracted from over 5600 sources, which include other databases, government documents, books, and scientific literature. "
            + "The focus of the T3DB is on providing mechanisms of toxicity and target proteins for each toxin. This dual nature of the T3DB, in which toxin and toxin target records are interactively linked in both directions, makes it unique from existing databases. It is also fully searchable and supports extensive text, sequence, chemical structure, and relational query searches.";
    private static final String TOXIN_LOGO = "http://neurolex.org/w/images/9/91/T3DB.png";
    private static final String TOXIN_ELT_URL = "http://t3db.org/toxins/";
    private static final String[] TOXIN_ITEMKEYS = {"toxinName", "description", "synonyms", "exposure", "mechanism_of_Action", "metabolism", "treatment", "health_Effects"};
    private static final Double[] TOXIN_WEIGHTS = {1.0, 0.9, 0.8, 0.8, 0.7, 0.7, 0.7, 0.9};
    private static final String[] TOXIN_ONTOIDS = {Structure.FOR_CONCEPT_RECOGNITION, Structure.FOR_CONCEPT_RECOGNITION, Structure.FOR_CONCEPT_RECOGNITION, Structure.FOR_CONCEPT_RECOGNITION, Structure.FOR_CONCEPT_RECOGNITION, Structure.FOR_CONCEPT_RECOGNITION, Structure.FOR_CONCEPT_RECOGNITION, Structure.FOR_CONCEPT_RECOGNITION};
    private static Structure TOXIN_STRUCTURE = new Structure(TOXIN_ITEMKEYS, TOXIN_RESOURCEID, TOXIN_WEIGHTS, TOXIN_ONTOIDS);
    private static String TOXIN_MAIN_ITEMKEY = "toxinName";

    // Constant for 'experiment' string
    private static final String nifId = "nif-0000-22933-1";
    private static final String ToxinName = "Toxin Name";
    private static final String Description = "Description";
    private static final String Synonyms = "Synonyms";
    private static final String Exposure = "Exposure";
    private static final String Mechanism_of_Action = "Mechanism of Action";
    private static final String Metabolism = "Metabolism";
    private static final String Treatment = "Treatment";
    private static final String HealthEffects = "Health Effects";
    private Map<String, String> localOntologyIDMap;

    // constructor
    public ToxinDBAccessTool() {
        super(TOXIN_NAME, TOXIN_RESOURCEID, TOXIN_STRUCTURE);
        try {
            this.getToolResource().setResourceURL(new URL(TOXIN_URL));
            this.getToolResource().setResourceDescription(TOXIN_DESCRIPTION);
            this.getToolResource().setResourceLogo(new URL(TOXIN_LOGO));
            this.getToolResource().setResourceElementURL(TOXIN_ELT_URL);
        } catch (MalformedURLException e) {
            logger.error(EMPTY_STRING, e);
        }
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
        return TOXIN_ELT_URL + elementLocalID;
    }

    @Override
    public String mainContextDescriptor() {
        return TOXIN_MAIN_ITEMKEY;
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
                // populates OBR_TOXIN_ET with each of these experiments.
                myExp = i.next();
                try {
                    if (!myExp.getElementStructure().hasNullValues()) {
                        if (this.resourceUpdateService.addElement(myExp)) {
                            nbElement++;
                        }
                    }
                } catch (Exception e) {
                    logger.error("** PROBLEM ** Problem with id " + myExp.getLocalElementId() + " when populating the OBR_TOXIN_ET table.", e);
                }
            }
        } catch (Exception e) {
            logger.error("** PROBLEM ** Cannot update resource " + this.getToolResource().getResourceName(), e);
        }
        logger.info(nbElement + " elements added to the OBR_TOXIN_ET table.");
        return nbElement;
    }

    /**
     * get all Elements.
     */
    public HashSet<Element> getAllElements() {
        logger.info("* Get All Elements for ToxinDB... ");
        HashSet<Element> elementSet = new HashSet<Element>();
        int nbAdded = 0;
        int offset = 0;
        int totalCount = 0;

        try {
            //get all elements from _ET table
            HashSet<String> allElementsInET = this.resourceUpdateService.getAllLocalElementIDs();
            Map<String, Map<String, String>> allRowsData = new HashMap<String, Map<String, String>>();

            //parsing data
            do {
                Document dom = queryFederation(nifId, query, offset, rowCount);
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
                            if (name.equalsIgnoreCase(ToxinName)) {                     //localElementId and Tittle
                                localElementId = value.substring(value.indexOf(TOXIN_ELT_URL) + TOXIN_ELT_URL.length(), value.indexOf(endTag));
                                elementAttributes.put(Structure.generateContextName(TOXIN_RESOURCEID, TOXIN_ITEMKEYS[0]), Jsoup.parse(value).text());
                            } else if (name.equalsIgnoreCase(Description)) {            //Description
                                elementAttributes.put(Structure.generateContextName(TOXIN_RESOURCEID, TOXIN_ITEMKEYS[1]), value);
                            } else if (name.equalsIgnoreCase(Synonyms)) {               //Synonyms
                                elementAttributes.put(Structure.generateContextName(TOXIN_RESOURCEID, TOXIN_ITEMKEYS[2]), value);
                            } else if (name.equalsIgnoreCase(Exposure)) {               //Exposure
                                elementAttributes.put(Structure.generateContextName(TOXIN_RESOURCEID, TOXIN_ITEMKEYS[3]), value);
                            } else if (name.equalsIgnoreCase(Mechanism_of_Action)) {    //Mechanism_of_Action
                                elementAttributes.put(Structure.generateContextName(TOXIN_RESOURCEID, TOXIN_ITEMKEYS[4]), value);
                            } else if (name.equalsIgnoreCase(Metabolism)) {             //Metabolism
                                elementAttributes.put(Structure.generateContextName(TOXIN_RESOURCEID, TOXIN_ITEMKEYS[5]), value);
                            } else if (name.equalsIgnoreCase(Treatment)) {              //Treatment
                                elementAttributes.put(Structure.generateContextName(TOXIN_RESOURCEID, TOXIN_ITEMKEYS[6]), value);
                            } else if (name.equalsIgnoreCase(HealthEffects)) {          //HealthEffects
                                elementAttributes.put(Structure.generateContextName(TOXIN_RESOURCEID, TOXIN_ITEMKEYS[7]), value);
                            }

                            //Check if elementId is present locally.
                            if (allElementsInET.contains(localElementId)) {
                                continue;
                            } else {
                                allRowsData.put(localElementId, elementAttributes);
                            }
                        }
                    }
                }
                offset += rowCount;
            } while (offset < totalCount);

            //parsing ends
            // Second phase: creation of elements

            for (String localElementID : allRowsData.keySet()) {
                Map<String, String> elementAttributes = new HashMap<String, String>();
                elementAttributes = allRowsData.get(localElementID);

                // PUT DATA INTO A STRUCTURE++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
                Structure elementStructure = new Structure(TOXIN_STRUCTURE.getContextNames());
                for (String contextName : TOXIN_STRUCTURE.getContextNames()) {
                    boolean attributeHasValue = false;

                    for (String att : elementAttributes.keySet()) {
                        if (contextName.equals(att)) {
                            // not an existing annotation
                            if (TOXIN_STRUCTURE.getOntoID(contextName).equals(Structure.FOR_CONCEPT_RECOGNITION)
                                    || TOXIN_STRUCTURE.getOntoID(contextName).equals(Structure.NOT_FOR_ANNOTATION)) {
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

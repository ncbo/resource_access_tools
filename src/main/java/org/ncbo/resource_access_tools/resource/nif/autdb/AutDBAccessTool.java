/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.ncbo.resource_access_tools.resource.nif.autdb;

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
 * AccessTool for NIF AutDB
 *
 * @author s.kharat
 */
public class AutDBAccessTool extends AbstractNifResourceAccessTool {

    private static final String AUTDB_URL = "http://autism.mindspec.org/autdb/Welcome.do";
    private static final String AUTDB_NAME = "AutDB (via NIF)";
    private static final String AUTDB_RESOURCEID = "AUTDB";
    private static final String AUTDB_DESCRIPTION = "AutDB is a public database of data from scientific publications for autism spectrum disorders. "
            + "It also includes several interactive modules that highlight the genes implicated in autism: Human Gene, Animal Models, and Protein Interactions.";
    private static final String AUTDB_LOGO = "http://autism.mindspec.org/autdb/images/logo1.png";
    private static final String AUTDB_ELT_URL = "http://autism.mindspec.org/animalmodel/";
    private static final String[] AUTDB_ITEMKEYS = {"Animal_Model", "Gene_Symbol", "Gene_Name", "Aliases", "References_for_Phenotype", "Phenotype_Profile"};
    private static final Double[] AUTDB_WEIGHTS = {1.0, 1.0, 1.0, 0.7, 0.5, 0.9};
    private static final String[] AUTDB_ONTOIDS = {Structure.NOT_FOR_ANNOTATION, Structure.FOR_CONCEPT_RECOGNITION, Structure.FOR_CONCEPT_RECOGNITION, Structure.FOR_CONCEPT_RECOGNITION, Structure.NOT_FOR_ANNOTATION, Structure.FOR_CONCEPT_RECOGNITION};
    private static final Structure AUTDB_STRUCTURE = new Structure(AUTDB_ITEMKEYS, AUTDB_RESOURCEID, AUTDB_WEIGHTS, AUTDB_ONTOIDS);
    // Constant
    private static final String nifId = "nif-0000-02587-1";
    private static final String Animal_Model = "Animal Model";
    private static final String Gene_Symbol = "Gene Symbol";
    private static final String Gene_Name = "Gene Name";
    private static final String Aliases = "Aliases";
    private static final String References_for_Phenotype = "Reference for Phenotype";
    private static final String Phenotype_Profile = "Phenotype Profile";

    // constructors
    public AutDBAccessTool() {
        super(AUTDB_NAME, AUTDB_RESOURCEID, AUTDB_STRUCTURE);
        try {
            this.getToolResource().setResourceURL(new URL(AUTDB_URL));
            this.getToolResource().setResourceDescription(AUTDB_DESCRIPTION);
            this.getToolResource().setResourceLogo(new URL(AUTDB_LOGO));
            this.getToolResource().setResourceElementURL(AUTDB_ELT_URL);
        } catch (MalformedURLException e) {
            logger.error(EMPTY_STRING, e);
        }
        Map<String, String> localOntologyIDMap = createLocalOntologyIDMap(AUTDB_STRUCTURE);
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
        return AUTDB_ELT_URL + elementLocalID;
    }

    @Override
    public String mainContextDescriptor() {
        String AUTDB_MAIN_ITEMKEY = "Animal_Model";
        return AUTDB_MAIN_ITEMKEY;
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
                // populates OBR_AutDB_ET with each of these experiments.
                myExp = i.next();
                try {
                    if (!myExp.getElementStructure().hasNullValues()) {
                        if (this.resourceUpdateService.addElement(myExp)) {
                            nbElement++;
                        }
                    }
                } catch (Exception e) {
                    logger.error("** PROBLEM ** Problem with id " + myExp.getLocalElementId() + " when populating the OBR_AutDB_ET table.", e);
                }
            }
        } catch (Exception e) {
            logger.error("** PROBLEM ** Cannot update resource " + this.getToolResource().getResourceName(), e);
        }
        logger.info(nbElement + " elements added to the OBR_AutDB_ET table.");
        return nbElement;
    }

    /**
     * get all Elements.
     */
    private HashSet<Element> getAllElements() {
        logger.info("* Get All Elements for AutDB... ");
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
                            if (name.equalsIgnoreCase(Animal_Model)) {                   //Gene_Symbol & localElementId
                                elementAttributes.put(Structure.generateContextName(AUTDB_RESOURCEID, AUTDB_ITEMKEYS[0]), Jsoup.parse(value).text());
                                localElementId = value.substring(value.indexOf(AUTDB_ELT_URL) + AUTDB_ELT_URL.length(), value.indexOf(endTag));
                            } else if (name.equalsIgnoreCase(Gene_Symbol)) {                 //Tissue
                                elementAttributes.put(Structure.generateContextName(AUTDB_RESOURCEID, AUTDB_ITEMKEYS[1]), Jsoup.parse(value).text());
                            } else if (name.equalsIgnoreCase(Gene_Name)) {               //Organism
                                elementAttributes.put(Structure.generateContextName(AUTDB_RESOURCEID, AUTDB_ITEMKEYS[2]), Jsoup.parse(value).text());
                            } else if (name.equalsIgnoreCase(Aliases)) {    //Experimental_factor
                                elementAttributes.put(Structure.generateContextName(AUTDB_RESOURCEID, AUTDB_ITEMKEYS[3]), value);
                            } else if (name.equalsIgnoreCase(References_for_Phenotype)) {         //Exp_vs_Control
                                elementAttributes.put(Structure.generateContextName(AUTDB_RESOURCEID, AUTDB_ITEMKEYS[4]), Jsoup.parse(value).text());
                            } else if (name.equalsIgnoreCase(Phenotype_Profile)) {        //Gene_expression
                                elementAttributes.put(Structure.generateContextName(AUTDB_RESOURCEID, AUTDB_ITEMKEYS[5]), value);
                            }
                        }

                        //Check if elementId is present locally.
                        if (allElementsInET.contains(localElementId)) {
                        } else {
                            if (allRowsData.containsKey(localElementId)) {
                                Map<String, String> oldElementAttributes = allRowsData.get(localElementId);
                                String oldPhenotypeProfile = oldElementAttributes.get(Structure.generateContextName(AUTDB_RESOURCEID, AUTDB_ITEMKEYS[5]));
                                String newPhenotypeValue = elementAttributes.get(Structure.generateContextName(AUTDB_RESOURCEID, AUTDB_ITEMKEYS[5]));
                                if (oldPhenotypeProfile.length() > 0 && newPhenotypeValue.length() > 0) {
                                    oldPhenotypeProfile += ", " + newPhenotypeValue;
                                }
                                oldElementAttributes.put(Structure.generateContextName(AUTDB_RESOURCEID, AUTDB_ITEMKEYS[5]), oldPhenotypeProfile);
                                allRowsData.put(localElementId, oldElementAttributes);
                            } else {
                                allRowsData.put(localElementId, elementAttributes);
                            }
                        }
                    }
                } else {
                    logger.info("Increase OFFSET: No Results for page : " + offset);
                    offset += rowCount;
                }
            } while (offset < totalCount);

            //parsing ends

            // Second phase: creation of elements
            for (String localElementID : allRowsData.keySet()) {
                Map<String, String> elementAttributes = new HashMap<String, String>();
                elementAttributes = allRowsData.get(localElementID);

                // PUT DATA INTO A STRUCTURE++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
                Structure elementStructure = new Structure(AUTDB_STRUCTURE.getContextNames());
                for (String contextName : AUTDB_STRUCTURE.getContextNames()) {
                    boolean attributeHasValue = false;

                    for (String att : elementAttributes.keySet()) {
                        if (contextName.equals(att)) {
                            // not an existing annotation
                            if (AUTDB_STRUCTURE.getOntoID(contextName).equals(Structure.FOR_CONCEPT_RECOGNITION)
                                    || AUTDB_STRUCTURE.getOntoID(contextName).equals(Structure.NOT_FOR_ANNOTATION)) {
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

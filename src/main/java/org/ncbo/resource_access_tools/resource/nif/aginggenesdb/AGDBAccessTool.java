/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.ncbo.resource_access_tools.resource.nif.aginggenesdb;

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
 * AccessTool for AgingGenesDB
 *
 * @author s.kharat
 */
public class AGDBAccessTool extends AbstractNifResourceAccessTool {

    private static final String AGDB_URL = "http://www.uwaging.org/genesdb/";
    private static final String AGDB_NAME = "AgingGenesDB (via NIF)";
    private static final String AGDB_RESOURCEID = "AGDB";
    private static final String AGDB_DESCRIPTION = "This database provides a searchable and browsable list of aging related genes and their effects on "
            + "longevity. Other information can also be found including: organism, aging phenotype, allele type, strain, gene function, phenotypes, mutant, "
            + "and homologs. The Biology of Aging at the University of Washington is the home of the Nathan Shock Center of Excellence in the Basic Biology "
            + "of Aging and the Genetic Approaches to Aging Training Grant. The University of Washington is a major center for research in Gerontology, and "
            + "these programs help to enrich the infrastructure for this work.";
    private static final String AGDB_LOGO = "http://neurolex.org/w/images/7/71/Aginggenesdb.png";
    private static final String AGDB_ELT_URL = "http://uwaging.org/genesdb/search?anyword=";
    private static final String[] AGDB_ITEMKEYS = {"Gene_Name", "Organism", "Aging_Phenotype", "Description", "Gene_function", "Other_Phenotypes", "Keywords", "Primary_Reference"};
    private static final Double[] AGDB_WEIGHTS = {1.0, 0.9, 0.9, 0.7, 0.7, 0.7, 0.7, 0.4};
    private static final String[] AGDB_ONTOIDS = {Structure.FOR_CONCEPT_RECOGNITION, Structure.FOR_CONCEPT_RECOGNITION, Structure.FOR_CONCEPT_RECOGNITION, Structure.FOR_CONCEPT_RECOGNITION, Structure.FOR_CONCEPT_RECOGNITION, Structure.FOR_CONCEPT_RECOGNITION, Structure.FOR_CONCEPT_RECOGNITION, Structure.NOT_FOR_ANNOTATION};
    private static final Structure AGDB_STRUCTURE = new Structure(AGDB_ITEMKEYS, AGDB_RESOURCEID, AGDB_WEIGHTS, AGDB_ONTOIDS);
    // Constant
    private static final String nifId = "nif-0000-23326-2";
    private static final String Gene_Name = "Gene Name";
    private static final String Organism = "Organism";
    private static final String Aging_Phenotype = "Aging Phenotype";
    private static final String Description = "Description";
    private static final String Gene_function = "Gene Function";
    private static final String Other_Phenotypes = "Other Phenotypes";
    private static final String Keywords = "Keywords";
    private static final String Primary_Reference = "Primary Reference";

    // constructors
    public AGDBAccessTool() {
        super(AGDB_NAME, AGDB_RESOURCEID, AGDB_STRUCTURE);
        try {
            this.getToolResource().setResourceURL(new URL(AGDB_URL));
            this.getToolResource().setResourceDescription(AGDB_DESCRIPTION);
            this.getToolResource().setResourceLogo(new URL(AGDB_LOGO));
            this.getToolResource().setResourceElementURL(AGDB_ELT_URL);
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
        return AGDB_ELT_URL + elementLocalID;
    }

    @Override
    public String mainContextDescriptor() {
        String AGDB_MAIN_ITEMKEY = "Gene_Name";
        return AGDB_MAIN_ITEMKEY;
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
                // populates OBR_AGDB_ET with each of these experiments.
                myExp = i.next();
                try {
                    if (!myExp.getElementStructure().hasNullValues()) {
                        if (this.resourceUpdateService.addElement(myExp)) {
                            nbElement++;
                        }
                    }
                } catch (Exception e) {
                    logger.error("** PROBLEM ** Problem with id " + myExp.getLocalElementId() + " when populating the OBR_AGDB_ET table.", e);
                }
            }
        } catch (Exception e) {
            logger.error("** PROBLEM ** Cannot update resource " + this.getToolResource().getResourceName(), e);
        }
        logger.info(nbElement + " elements added to the OBR_AGDB_ET table.");
        return nbElement;
    }

    /**
     * get all Elements.
     */
    private HashSet<Element> getAllElements() {
        logger.info("* Get All Elements for AgingGenesDB... ");
        HashSet<Element> elementSet = new HashSet<Element>();
        int nbAdded;
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
                            if (name.equalsIgnoreCase(Gene_Name)) {                       //Gene_Name & localElementId
                                elementAttributes.put(Structure.generateContextName(AGDB_RESOURCEID, AGDB_ITEMKEYS[0]), Jsoup.parse(value).text());
                                localElementId = value.substring(value.indexOf(AGDB_ELT_URL) + AGDB_ELT_URL.length(), value.indexOf(endTag));
                            } else if (name.equalsIgnoreCase(Organism)) {                 //Organism
                                elementAttributes.put(Structure.generateContextName(AGDB_RESOURCEID, AGDB_ITEMKEYS[1]), value);
                            } else if (name.equalsIgnoreCase(Aging_Phenotype)) {          //Aging_Phenotype
                                elementAttributes.put(Structure.generateContextName(AGDB_RESOURCEID, AGDB_ITEMKEYS[2]), value);
                            } else if (name.equalsIgnoreCase(Description)) {              //Description
                                elementAttributes.put(Structure.generateContextName(AGDB_RESOURCEID, AGDB_ITEMKEYS[3]), value);
                            } else if (name.equalsIgnoreCase(Gene_function)) {            //Gene_function
                                elementAttributes.put(Structure.generateContextName(AGDB_RESOURCEID, AGDB_ITEMKEYS[4]), value);
                            } else if (name.equalsIgnoreCase(Other_Phenotypes)) {         //Other_Phenotypes
                                elementAttributes.put(Structure.generateContextName(AGDB_RESOURCEID, AGDB_ITEMKEYS[5]), value);
                            } else if (name.equalsIgnoreCase(Keywords)) {                 //Keywords
                                elementAttributes.put(Structure.generateContextName(AGDB_RESOURCEID, AGDB_ITEMKEYS[6]), value);
                            } else if (name.equalsIgnoreCase(Primary_Reference)) {       //Primary_Reference
                                elementAttributes.put(Structure.generateContextName(AGDB_RESOURCEID, AGDB_ITEMKEYS[7]), value);
                            }
                        }

                        //Check if elementId is present locally.
                        if (allElementsInET.contains(localElementId)) {
                        } else {
                            allRowsData.put(localElementId, elementAttributes);
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
                Map<String, String> elementAttributes;
                elementAttributes = allRowsData.get(localElementID);

                // PUT DATA INTO A STRUCTURE++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
                Structure elementStructure = new Structure(AGDB_STRUCTURE.getContextNames());
                for (String contextName : AGDB_STRUCTURE.getContextNames()) {
                    boolean attributeHasValue = false;

                    for (String att : elementAttributes.keySet()) {
                        if (contextName.equals(att)) {
                            // not an existing annotation
                            if (AGDB_STRUCTURE.getOntoID(contextName).equals(Structure.FOR_CONCEPT_RECOGNITION)
                                    || AGDB_STRUCTURE.getOntoID(contextName).equals(Structure.NOT_FOR_ANNOTATION)) {
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

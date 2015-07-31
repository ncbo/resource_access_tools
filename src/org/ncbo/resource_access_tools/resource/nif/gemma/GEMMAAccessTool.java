/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.ncbo.resource_access_tools.resource.nif.gemma;

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
 * AccessTool for NIF GEMMA
 * @author s.kharat
 */
public class GEMMAAccessTool extends AbstractNifResourceAccessTool {

    private static final String GEMMA_URL = "http://www.chibi.ubc.ca/Gemma/home.html";
    private static final String GEMMA_NAME = "GEMMA (via NIF)";
    private static final String GEMMA_RESOURCEID = "GEMMA";
    private static final String GEMMA_DESCRIPTION = "Gemma is a database and software system for the meta-analysis of gene expression data. Gemma contains "
            + "data from hundreds of public microarray data sets, referencing hundreds of published papers. Users can search, access and visualize "
            + "coexpression and differential expression results.";
    private static final String GEMMA_LOGO = "http://neurolex.org/w/images/0/08/Gemma.gif";
    private static final String GEMMA_ELT_URL = "http://www.chibi.ubc.ca/Gemma/expressionExperiment/showExpressionExperiment.html?id=";
    private static final String[] GEMMA_ITEMKEYS = {"Source", "differentially_expressed_genes_in_listed_tissues", "Tissue", "Organism", "Description", "Array_Platform"};
    private static final Double[] GEMMA_WEIGHTS = {1.0, 0.9, 0.9, 0.9, 0.7, 0.4};
    private static final String[] GEMMA_ONTOIDS = {Structure.NOT_FOR_ANNOTATION, Structure.FOR_CONCEPT_RECOGNITION, Structure.FOR_CONCEPT_RECOGNITION, Structure.FOR_CONCEPT_RECOGNITION, Structure.FOR_CONCEPT_RECOGNITION, Structure.NOT_FOR_ANNOTATION};
    private static Structure GEMMA_STRUCTURE = new Structure(GEMMA_ITEMKEYS, GEMMA_RESOURCEID, GEMMA_WEIGHTS, GEMMA_ONTOIDS);
    private static String GEMMA_MAIN_ITEMKEY = "Source";
    // Constant
    private static final String nifId = "nif-0000-08127-1";
    private static final String Gene_Symbol = "Gene Symbol";
    private static final String Tissue = "Tissue";
    private static final String Organism = "Organism";
    private static final String Description = "Description";
    private static final String Source = "Source";
    private static final String Array_Platform = "Array Platform";
    private Map<String, String> localOntologyIDMap;

    // constructors
    public GEMMAAccessTool() {
        super(GEMMA_NAME, GEMMA_RESOURCEID, GEMMA_STRUCTURE);
        try {
            this.getToolResource().setResourceURL(new URL(GEMMA_URL));
            this.getToolResource().setResourceDescription(GEMMA_DESCRIPTION);
            this.getToolResource().setResourceLogo(new URL(GEMMA_LOGO));
            this.getToolResource().setResourceElementURL(GEMMA_ELT_URL);
        } catch (MalformedURLException e) {
            logger.error(EMPTY_STRING, e);
        }
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
        return GEMMA_ELT_URL + elementLocalID;
    }

    @Override
    public String mainContextDescriptor() {
        return GEMMA_MAIN_ITEMKEY;
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
                // populates OBR_GEMMA_ET with each of these experiments.
                myExp = i.next();
                try {
                    if (!myExp.getElementStructure().hasNullValues()) {
                        if (this.resourceUpdateService.addElement(myExp)) {
                            nbElement++;
                        }
                    }
                } catch (Exception e) {
                    logger.error("** PROBLEM ** Problem with id " + myExp.getLocalElementId() + " when populating the OBR_GEMMA_ET table.", e);
                }
            }
        } catch (Exception e) {
            logger.error("** PROBLEM ** Cannot update resource " + this.getToolResource().getResourceName(), e);
        }
        logger.info(nbElement + " elements added to the OBR_GEMMA_ET table.");
        return nbElement;
    }

    /**
     * get all Elements.
     */
    public HashSet<Element> getAllElements() {
        logger.info("* Get All Elements for GEEMA... ");
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

                            if (name.equalsIgnoreCase(Source)) {                        //Source & localElementId
                                elementAttributes.put(Structure.generateContextName(GEMMA_RESOURCEID, GEMMA_ITEMKEYS[0]), Jsoup.parse(value).text());
                                localElementId = value.substring(value.indexOf(GEMMA_ELT_URL) + GEMMA_ELT_URL.length(), value.indexOf(endTag));
                            } else if (name.equalsIgnoreCase(Gene_Symbol)) {            //Gene_Symbol
                                elementAttributes.put(Structure.generateContextName(GEMMA_RESOURCEID, GEMMA_ITEMKEYS[1]), Jsoup.parse(value).text());
                            } else if (name.equalsIgnoreCase(Tissue)) {                 //Tissue
                                elementAttributes.put(Structure.generateContextName(GEMMA_RESOURCEID, GEMMA_ITEMKEYS[2]), value);
                            } else if (name.equalsIgnoreCase(Organism)) {               //Organism
                                elementAttributes.put(Structure.generateContextName(GEMMA_RESOURCEID, GEMMA_ITEMKEYS[3]), value);
                            } else if (name.equalsIgnoreCase(Description)) {            //Description
                                elementAttributes.put(Structure.generateContextName(GEMMA_RESOURCEID, GEMMA_ITEMKEYS[4]), value);
                            } else if (name.equalsIgnoreCase(Array_Platform)) {         //Array_Platform
                                elementAttributes.put(Structure.generateContextName(GEMMA_RESOURCEID, GEMMA_ITEMKEYS[5]), value);
                            }
                        }

                        //Check if elementId is present locally.
                        if (allElementsInET.contains(localElementId)) {
                            continue;
                        } else {
                            if (allRowsData.containsKey(localElementId)) {
                                //comma separated tissues
                                String previousTissue = allRowsData.get(localElementId).get(Structure.generateContextName(GEMMA_RESOURCEID, GEMMA_ITEMKEYS[2]));
                                String currentTissue = elementAttributes.get(Structure.generateContextName(GEMMA_RESOURCEID, GEMMA_ITEMKEYS[2]));

                                if (previousTissue.length() > 0 && currentTissue.length() > 0 && !previousTissue.contains(currentTissue)) {
                                    previousTissue += "," + currentTissue;
                                }
                                allRowsData.get(localElementId).put(Structure.generateContextName(GEMMA_RESOURCEID, GEMMA_ITEMKEYS[2]), previousTissue);

                                //comma separated gene symbols
                                String currentGeneSymbol = elementAttributes.get(Structure.generateContextName(GEMMA_RESOURCEID, GEMMA_ITEMKEYS[1]));
                                String previousGeneSymbol = allRowsData.get(localElementId).get(Structure.generateContextName(GEMMA_RESOURCEID, GEMMA_ITEMKEYS[1]));

                                if (previousGeneSymbol.length() > 0 && currentGeneSymbol.length() > 0 && !previousGeneSymbol.contains(currentGeneSymbol)) {
                                    previousGeneSymbol += "," + currentGeneSymbol;
                                }
                                allRowsData.get(localElementId).put(Structure.generateContextName(GEMMA_RESOURCEID, GEMMA_ITEMKEYS[1]), previousGeneSymbol);
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
                Structure elementStructure = new Structure(GEMMA_STRUCTURE.getContextNames());
                for (String contextName : GEMMA_STRUCTURE.getContextNames()) {
                    boolean attributeHasValue = false;

                    for (String att : elementAttributes.keySet()) {
                        if (contextName.equals(att)) {
                            // not an existing annotation
                            if (GEMMA_STRUCTURE.getOntoID(contextName).equals(Structure.FOR_CONCEPT_RECOGNITION)
                                    || GEMMA_STRUCTURE.getOntoID(contextName).equals(Structure.NOT_FOR_ANNOTATION)) {
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

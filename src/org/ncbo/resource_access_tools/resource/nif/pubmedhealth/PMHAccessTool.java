/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.ncbo.resource_access_tools.resource.nif.pubmedhealth;

import org.ncbo.resource_access_tools.resource.nif.AbstractNifResourceAccessTool;
import org.ncbo.resource_access_tools.resource.nif.AbstractNifResourceAccessTool;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import obs.obr.populate.Element;
import obs.obr.populate.Structure;
import org.jsoup.Jsoup;
import org.ncbo.stanford.obr.enumeration.ResourceType;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * AccessTool for NIF PubMedHealth Drugs
 * @author s.kharat
 */
public class PMHAccessTool extends AbstractNifResourceAccessTool {

    private static final String PMH_URL = "http://www.ncbi.nlm.nih.gov/pubmedhealth";
    private static final String PMH_NAME = "PubMedHealth Drugs (via NIF)";
    private static final String PMH_RESOURCEID = "PMH";
    private static final String PMH_DESCRIPTION = "PubMed Health is a consumer health Web site produced by the National Center for Biotechnology Information (NCBI), a division of the National Library of Medicine (NLM) at the National Institutes of Health (NIH). PubMed Health provides up-to-date information on diseases, conditions, injuries, drugs, supplements, treatment options, and healthy living, with a special focus on comparative effectiveness research from institutions around the world.";
    private static final String PMH_LOGO = "http://neurolex.org/w/images/8/8c/PubMed_Health.PNG";
    private static final String PMH_ELT_URL = "http://www.ncbi.nlm.nih.gov/pubmedhealth/";
    private static final String[] PMH_ITEMKEYS = {"Name", "How_to_use", "Side_effects", "Other_information", "Why_is_this_medication_prescribed", "Storage_conditions", "In_case_of_emergency", "If_I_forgot_a_dose", "Special_dietary_instructions", "Other_uses"};
    private static final Double[] PMH_WEIGHTS = {1.0, 0.9, 0.9, 0.8, 0.8, 0.7, 0.9, 0.8, 0.7, 0.7};
    private static final String[] PMH_ONTOIDS = {Structure.FOR_CONCEPT_RECOGNITION, Structure.FOR_CONCEPT_RECOGNITION, Structure.FOR_CONCEPT_RECOGNITION, Structure.FOR_CONCEPT_RECOGNITION,
        Structure.FOR_CONCEPT_RECOGNITION, Structure.FOR_CONCEPT_RECOGNITION, Structure.FOR_CONCEPT_RECOGNITION, Structure.FOR_CONCEPT_RECOGNITION,
        Structure.FOR_CONCEPT_RECOGNITION, Structure.FOR_CONCEPT_RECOGNITION};
    private static Structure PMH_STRUCTURE = new Structure(PMH_ITEMKEYS, PMH_RESOURCEID, PMH_WEIGHTS, PMH_ONTOIDS);
    private static String PMH_MAIN_ITEMKEY = "Name";
    // Constants
    private static final String nifId = "nlx_32805-3";
    private static final String pmh_title = "Title";
    private static final String pmh_text = "Text";
    private static final String PMH_Name = "Name";
    private static final String How_to_use = "How should this medicine be used?";
    private static final String Side_effects = "What side effects can this medication cause?";
    private static final String Other_information = "What other information should I know?";
    private static final String Why_is_this_medication_prescribed = "Why is this medication prescribed?";
    private static final String Storage_conditions = "What storage conditions are needed for this medicine?";
    private static final String In_case_of_emergency = "In case of emergency/overdose";
    private static final String If_I_forgot_a_dose = "What should I do if I forget a dose?";
    private static final String Special_dietary_instructions = "What special dietary instructions should I follow?";
    private static final String Other_uses = "Other uses for this medicine";
    private Map<String, String> localOntologyIDMap;

    // constructors
    public PMHAccessTool() {
        super(PMH_NAME, PMH_RESOURCEID, PMH_STRUCTURE);
        try {
            this.getToolResource().setResourceURL(new URL(PMH_URL));
            this.getToolResource().setResourceDescription(PMH_DESCRIPTION);
            this.getToolResource().setResourceLogo(new URL(PMH_LOGO));
            this.getToolResource().setResourceElementURL(PMH_ELT_URL);
        } catch (MalformedURLException e) {
            logger.error(EMPTY_STRING, e);
        }
        localOntologyIDMap = createLocalOntologyIDMap(PMH_STRUCTURE);
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
        return PMH_ELT_URL + elementLocalID;
    }

    @Override
    public String mainContextDescriptor() {
        return PMH_MAIN_ITEMKEY;
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
                // populates OBR_PMH_ET with each of these experiments.
                myExp = i.next();
                try {
                    if (!myExp.getElementStructure().hasNullValues()) {
                        if (this.resourceUpdateService.addElement(myExp)) {
                            nbElement++;
                        }
                    }
                } catch (Exception e) {
                    logger.error("** PROBLEM ** Problem with id " + myExp.getLocalElementId() + " when populating the OBR_PMH_ET table.", e);
                }
            }
        } catch (Exception e) {
            logger.error("** PROBLEM ** Cannot update resource " + this.getToolResource().getResourceName(), e);
        }
        logger.info(nbElement + " elements added to the OBR_PMH_ET table.");
        return nbElement;
    }

    /**
     * get all Elements.
     */
    public HashSet<Element> getAllElements() {
        logger.info("* Get All Elements for PubMedHealth Drugs... ");
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
                        String attTitle = EMPTY_STRING;
                        String attValue = EMPTY_STRING;
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
                            if (name.equalsIgnoreCase(PMH_Name)) {
                                localElementId = value.substring(value.indexOf(PMH_ELT_URL) + PMH_ELT_URL.length(), value.indexOf(endTag) - 1);
                                elementAttributes.put(Structure.generateContextName(PMH_RESOURCEID, PMH_ITEMKEYS[0]), Jsoup.parse(value).text());
                            } else if (name.equalsIgnoreCase(pmh_text)) {
                                attValue = value;
                            } else if (name.equalsIgnoreCase(pmh_title)) {
                                attTitle = value;
                            }
                        }


                        if (attTitle.equalsIgnoreCase(How_to_use) || attTitle.contains("How to Use")) {//|| attTitle.contains("About your treatment") || attTitle.contains("Administering your medication")
                            elementAttributes.put(Structure.generateContextName(PMH_RESOURCEID, PMH_ITEMKEYS[1]), attValue);
                        } else if (attTitle.equalsIgnoreCase(Side_effects) || attTitle.contains("risks")) {
                            elementAttributes.put(Structure.generateContextName(PMH_RESOURCEID, PMH_ITEMKEYS[2]), attValue);
                        } else if (attTitle.equalsIgnoreCase(Other_information)) {
                            elementAttributes.put(Structure.generateContextName(PMH_RESOURCEID, PMH_ITEMKEYS[3]), attValue);
                        } else if (attTitle.equalsIgnoreCase(Why_is_this_medication_prescribed)) {//|| attTitle.contains("What is")
                            elementAttributes.put(Structure.generateContextName(PMH_RESOURCEID, PMH_ITEMKEYS[4]), attValue);
                        } else if (attTitle.equalsIgnoreCase(Storage_conditions)) {
                            elementAttributes.put(Structure.generateContextName(PMH_RESOURCEID, PMH_ITEMKEYS[5]), attValue);
                        } else if (attTitle.equalsIgnoreCase(In_case_of_emergency) || attTitle.equalsIgnoreCase("severe reaction")) {
                            elementAttributes.put(Structure.generateContextName(PMH_RESOURCEID, PMH_ITEMKEYS[6]), attValue);
                        } else if (attTitle.equalsIgnoreCase(If_I_forgot_a_dose)) {
                            elementAttributes.put(Structure.generateContextName(PMH_RESOURCEID, PMH_ITEMKEYS[7]), attValue);
                        } else if (attTitle.equalsIgnoreCase(Special_dietary_instructions)) {
                            elementAttributes.put(Structure.generateContextName(PMH_RESOURCEID, PMH_ITEMKEYS[8]), attValue);
                        } else if (attTitle.equalsIgnoreCase(Other_uses)) {                //Title
                            elementAttributes.put(Structure.generateContextName(PMH_RESOURCEID, PMH_ITEMKEYS[9]), attValue);
                        }

                        //Check if elementId is present locally.
                        if (allElementsInET.contains(localElementId)) {
                            continue;
                        } else {
                            if (allRowsData.containsKey(localElementId)) {
                                Map<String, String> prevAttributes = allRowsData.get(localElementId);
                                for (String attribute : elementAttributes.keySet()) {
                                    prevAttributes.put(attribute, elementAttributes.get(attribute));
                                }
                                allRowsData.put(localElementId, prevAttributes);
                            } else {
                                allRowsData.put(localElementId, elementAttributes);
                            }
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
                Structure elementStructure = new Structure(PMH_STRUCTURE.getContextNames());
                for (String contextName : PMH_STRUCTURE.getContextNames()) {
                    boolean attributeHasValue = false;

                    for (String att : elementAttributes.keySet()) {
                        if (contextName.equals(att)) {
                            // not an existing annotation
                            if (PMH_STRUCTURE.getOntoID(contextName).equals(Structure.FOR_CONCEPT_RECOGNITION)
                                    || PMH_STRUCTURE.getOntoID(contextName).equals(Structure.NOT_FOR_ANNOTATION)) {
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

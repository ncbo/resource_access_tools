/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.ncbo.resource_access_tools.resource.nif.pubmedhealth.tests;

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
 * AccessTool for NIF PubMedHealth Tests
 * @author s.kharat
 */
public class PMHTAccessTool extends AbstractNifResourceAccessTool {

    private static final String PMH_URL = "http://www.ncbi.nlm.nih.gov/pubmedhealth";
    private static final String PMH_NAME = "PubMedHealth Tests (via NIF)";
    private static final String PMH_RESOURCEID = "PMHT";
    private static final String PMH_DESCRIPTION = "PubMed Health, a consumer health Web site, provides up-to-date information on diseases, conditions, "
            + "injuries, drugs, supplements, treatment options, and healthy living, with a special focus on comparative effectiveness research from "
            + "institutions around the world. PubMed Health includes consumer guides summarizing comparative effectiveness research, fact sheets on "
            + "diseases and conditions, information on drugs and supplements, encyclopedic overviews of health topics, links to external Web sites. "
            + "PubMed Health has a special focus on comparative effectiveness research, in particular that research which evaluates the available "
            + "evidence of the benefits and harms of different treatment options for different groups of people. In Comparative Effectiveness "
            + "Research, experts often synthesize the evidence from dozens, or even hundreds, of individual studies.";
    private static final String PMH_LOGO = "http://neurolex.org/w/images/8/8c/PubMed_Health.PNG";
    private static final String PMH_ELT_URL = "http://www.ncbi.nlm.nih.gov/pubmedhealth/";
    private static final String[] PMH_ITEMKEYS = {"Test_Name", "Why_the_test_is_performed", "How_the_test_is_performed", "How_the_test_will_feel",
        "How_to_prepare_for_the_test", "What_abnormal_results_mean", "Normal_Values", "Risks", "Special_considerations", "Description", "Prognosis", "Before_the_Procedure", "After_the_Procedure"};
    private static final Double[] PMH_WEIGHTS = {1.0, 0.9, 0.9, 0.9, 0.9, 0.9, 0.9, 0.9, 0.8, 0.7, 0.6, 0.6, 0.6};
    private static final String[] PMH_ONTOIDS = {Structure.FOR_CONCEPT_RECOGNITION, Structure.FOR_CONCEPT_RECOGNITION, Structure.FOR_CONCEPT_RECOGNITION, Structure.FOR_CONCEPT_RECOGNITION, Structure.FOR_CONCEPT_RECOGNITION, Structure.FOR_CONCEPT_RECOGNITION, Structure.FOR_CONCEPT_RECOGNITION, Structure.FOR_CONCEPT_RECOGNITION, Structure.FOR_CONCEPT_RECOGNITION, Structure.FOR_CONCEPT_RECOGNITION, Structure.FOR_CONCEPT_RECOGNITION, Structure.FOR_CONCEPT_RECOGNITION, Structure.FOR_CONCEPT_RECOGNITION};
    private static Structure PMH_STRUCTURE = new Structure(PMH_ITEMKEYS, PMH_RESOURCEID, PMH_WEIGHTS, PMH_ONTOIDS);
    private static String PMH_MAIN_ITEMKEY = "Test_Name";
    // Constant
    private static final String nifId = "nlx_32805-2";
    private static final String PMH_Name = "Test Name";
    private static final String pmh_title = "Title";
    private static final String pmh_text = "Text";
    private static final String Why_the_test_is_performed = "Why the test is performed";
    private static final String Why_the_test_is_performed1 = "Why the Procedure Is Performed";
    private static final String How_the_test_is_performed = "How the test is performed";
    private static final String How_the_test_will_feel = "How the test will feel";
    private static final String How_to_prepare_for_the_test = "How to prepare for the test";
    private static final String What_abnormal_results_mean = "What abnormal results mean";
    private static final String Normal_Values = "Normal Values";
    private static final String Risks = "What the risks are";
    private static final String Risks1 = "Risks";
    private static final String Special_considerations = "Special considerations";
    private static final String Description = "Description";
    private static final String Description1 = "Information";
    private static final String Prognosis = "Outlook (Prognosis)";
    private static final String Before_the_Procedure = "Before the Procedure";
    private static final String After_the_Procedure = "After the Procedure";
    private Map<String, String> localOntologyIDMap;

    // constructors
    public PMHTAccessTool() {
        super(PMH_NAME, PMH_RESOURCEID, PMH_STRUCTURE);
        try {
            this.getToolResource().setResourceURL(new URL(PMH_URL));
            this.getToolResource().setResourceDescription(PMH_DESCRIPTION);
            this.getToolResource().setResourceLogo(new URL(PMH_LOGO));
            this.getToolResource().setResourceElementURL(PMH_ELT_URL);
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
        String[] elementId = elementLocalID.split(SLASH_STRING);
        return PMH_ELT_URL + elementId[0];
    }

    @Override
    public String mainContextDescriptor() {
        return PMH_MAIN_ITEMKEY;
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
        logger.info("* Get All Elements for PubMedHealth Tests... ");
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


                        if (attTitle.equalsIgnoreCase(Why_the_test_is_performed) || attTitle.contains(Why_the_test_is_performed1)) {
                            elementAttributes.put(Structure.generateContextName(PMH_RESOURCEID, PMH_ITEMKEYS[1]), attValue);
                        } else if (attTitle.equalsIgnoreCase(How_the_test_is_performed)) {
                            elementAttributes.put(Structure.generateContextName(PMH_RESOURCEID, PMH_ITEMKEYS[2]), attValue);
                        } else if (attTitle.equalsIgnoreCase(How_the_test_will_feel)) {
                            elementAttributes.put(Structure.generateContextName(PMH_RESOURCEID, PMH_ITEMKEYS[3]), attValue);
                        } else if (attTitle.equalsIgnoreCase(How_to_prepare_for_the_test)) {
                            elementAttributes.put(Structure.generateContextName(PMH_RESOURCEID, PMH_ITEMKEYS[4]), attValue);
                        } else if (attTitle.equalsIgnoreCase(What_abnormal_results_mean)) {
                            elementAttributes.put(Structure.generateContextName(PMH_RESOURCEID, PMH_ITEMKEYS[5]), attValue);
                        } else if (attTitle.equalsIgnoreCase(Normal_Values)) {
                            elementAttributes.put(Structure.generateContextName(PMH_RESOURCEID, PMH_ITEMKEYS[6]), attValue);
                        } else if (attTitle.equalsIgnoreCase(Risks) || attTitle.equalsIgnoreCase(Risks1)) {
                            elementAttributes.put(Structure.generateContextName(PMH_RESOURCEID, PMH_ITEMKEYS[7]), attValue);
                        } else if (attTitle.equalsIgnoreCase(Special_considerations)) {
                            elementAttributes.put(Structure.generateContextName(PMH_RESOURCEID, PMH_ITEMKEYS[8]), attValue);
                        } else if (attTitle.equalsIgnoreCase(Description) || attTitle.equalsIgnoreCase(Description1)) {
                            elementAttributes.put(Structure.generateContextName(PMH_RESOURCEID, PMH_ITEMKEYS[9]), attValue);
                        } else if (attTitle.equalsIgnoreCase(Prognosis)) {
                            elementAttributes.put(Structure.generateContextName(PMH_RESOURCEID, PMH_ITEMKEYS[10]), attValue);
                        } else if (attTitle.equalsIgnoreCase(Before_the_Procedure)) {
                            elementAttributes.put(Structure.generateContextName(PMH_RESOURCEID, PMH_ITEMKEYS[11]), attValue);
                        } else if (attTitle.equalsIgnoreCase(After_the_Procedure)) {                //Title
                            elementAttributes.put(Structure.generateContextName(PMH_RESOURCEID, PMH_ITEMKEYS[12]), attValue);
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

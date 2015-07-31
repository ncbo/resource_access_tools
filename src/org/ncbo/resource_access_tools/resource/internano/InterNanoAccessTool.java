/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.ncbo.resource_access_tools.resource.internano;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;

import org.ncbo.resource_access_tools.enumeration.ResourceType;
import org.ncbo.resource_access_tools.populate.Element;
import org.ncbo.resource_access_tools.populate.Structure;
import org.ncbo.resource_access_tools.resource.AbstractXmlResourceAccessTool;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

/**
 * Resource Access Tool for InterNano resource
 * @author s.kharat
 */
public class InterNanoAccessTool extends AbstractXmlResourceAccessTool {

    private static final String INPD_URL = "http://www.internano.org";
    private static final String INPD_NAME = "InterNano Process Database";
    private static final String INPD_RESOURCEID = "INPD";
    private static final String INPD_DESCRIPTION = "The InterNano Process Database is a knowledge base of techniques for processing nanoscale materials, "
            + "devices, and structures that includes step-by-step descriptions, images, notes on methodology and environmental variables, and associated "
            + "references and patent information. The purpose of the Process Database is to facilitate the sharing of appropriate process knowledge across "
            + "laboratories. The processes included here have been previously published or patented.";
    private static final String INPD_LOGO = "http://www.internano.org/images/InterNano.png";
    private static final String INPD_ELT_URL = "http://www.internano.org/component/option,com_process/task,view/id,";
    private static final String[] INPD_ITEMKEYS = {"Name", "Description", "Terms"};
    private static final Double[] INPD_WEIGHTS = {1.0, 0.7, 0.9};
    private static final String[] INPD_ONTOIDS = {Structure.FOR_CONCEPT_RECOGNITION, Structure.FOR_CONCEPT_RECOGNITION, Structure.FOR_CONCEPT_RECOGNITION};
    private static Structure STRUCTURE = new Structure(INPD_ITEMKEYS, INPD_RESOURCEID, INPD_WEIGHTS, INPD_ONTOIDS);
    private static String INPD_MAIN_ITEMKEY = "Name";
    private Map<String, String> localOntologyIDMap;
    //constants
    private static final String service_URL = "http://www.internano.org/rat_generate.php";
    private static final String elementTag = "element";
    private static final String localelementIdTag = "localElementId";
    private static final String nameTag = "name";
    private static final String descriptionTag = "description";
    private static final String termsTag = "terms";

    // constructors
    public InterNanoAccessTool() {
        super(INPD_NAME, INPD_RESOURCEID, STRUCTURE);
        try {
            this.getToolResource().setResourceURL(new URL(INPD_URL));
            this.getToolResource().setResourceDescription(INPD_DESCRIPTION);
            this.getToolResource().setResourceLogo(new URL(INPD_LOGO));
            this.getToolResource().setResourceElementURL(INPD_ELT_URL);
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
        return INPD_ELT_URL + elementLocalID;
    }

    @Override
    public String mainContextDescriptor() {
        return INPD_MAIN_ITEMKEY;
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
                // populates OBR_BIOM_ET with each of these experiments.
                myExp = i.next();
                try {
                    if (!myExp.getElementStructure().hasNullValues()) {
                        if (this.resourceUpdateService.addElement(myExp)) {
                            nbElement++;
                        }
                    }
                } catch (Exception e) {
                    logger.error("** PROBLEM ** Problem with id " + myExp.getLocalElementId() + " when populating the OBR_INPD_ET table.", e);
                }
            }
        } catch (Exception e) {
            logger.error("** PROBLEM ** Cannot update resource " + this.getToolResource().getResourceName(), e);
        }
        logger.info(nbElement + " elements added to the OBR_INPD_ET table.");
        return nbElement;
    }

    /**
     * get all Elements.
     */
    public HashSet<Element> getAllElements() {
        logger.info("* Get All Elements for InerNano ... ");
        HashSet<Element> elementSet = new HashSet<Element>();
        int nbAdded = 0;
        String localElementId = EMPTY_STRING;
        Document dom = null;

        try {
            //get all elements from _ET table
            HashSet<String> allElementsInET = this.resourceUpdateService.getAllLocalElementIDs();
            Map<String, Map<String, String>> allRowsData = new HashMap<String, Map<String, String>>();

            dom = parseXML(service_URL);
            NodeList elementNodeList = dom.getElementsByTagName(elementTag);

            for (int i = 0; i < elementNodeList.getLength(); i++) {
                Map<String, String> elementAttributes = new HashMap<String, String>();
                NodeList elementChilds = elementNodeList.item(i).getChildNodes();
                for (int j = 0; j < elementChilds.getLength(); j++) {
                    if (elementChilds.item(j).getNodeName().equalsIgnoreCase(localelementIdTag)) {
                        localElementId = elementChilds.item(j).getChildNodes().item(0).getNodeValue();
                    } else if (elementChilds.item(j).getNodeName().equalsIgnoreCase(nameTag)) {
                        elementAttributes.put(Structure.generateContextName(INPD_RESOURCEID, INPD_ITEMKEYS[0]), elementChilds.item(j).getChildNodes().item(0).getNodeValue());
                    } else if (elementChilds.item(j).getNodeName().equalsIgnoreCase(descriptionTag)) {
                        elementAttributes.put(Structure.generateContextName(INPD_RESOURCEID, INPD_ITEMKEYS[1]), elementChilds.item(j).getChildNodes().item(0).getNodeValue());
                    } else if (elementChilds.item(j).getNodeName().equalsIgnoreCase(termsTag)) {
                        elementAttributes.put(Structure.generateContextName(INPD_RESOURCEID, INPD_ITEMKEYS[2]), elementChilds.item(j).getChildNodes().item(0).getNodeValue());
                    }
                }
                //Check if elementId is present in database.
                if (allElementsInET.contains(localElementId)) {
                    continue;
                } else {
                    allRowsData.put(localElementId, elementAttributes);
                }
            }

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

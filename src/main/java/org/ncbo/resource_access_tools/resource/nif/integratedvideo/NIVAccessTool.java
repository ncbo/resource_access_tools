/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.ncbo.resource_access_tools.resource.nif.integratedvideo;

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
 * AccessTool for NIF Integrated Videos
 *
 * @author s.kharat
 */
public class NIVAccessTool extends AbstractNifResourceAccessTool {

    private static final String URL = "http://videocast.nih.gov/";
    private static final String NAME = "Integrated Videos (via NIF)";
    private static final String RESOURCE_ID = "NIV";
    private static final String DESCRIPTION = "The NIF Integrated Video View is a virtual database currently indexing video and other multimedia content "
            + "from NIH VideoCasting and Podcasting, JoVE: Journal of Visualized Experiments, The Guardian: Science Videos and Biointeractive. "
            + "JoVE is a peer reviewed, PubMed indexed journal devoted to the publication of biological research in a video format and NIH video "
            + "is a vast archive of scientific talks given at various NIH meetings and functions.";
    private static final String LOGO = "http://neurolex.org/w/images/e/e0/NIF_Integrated_Video.PNG";
    private static final String ELT_URL = "";
    private static final String[] ITEMKEYS = {"Title", "Description", "Author", "Database", "Category", "Date_published", "Runtime"};
    private static final Double[] WEIGHTS = {1.0, 0.9, 0.0, 0.0, 0.0, 0.0, 0.0};
    private static final String[] ONTOIDS = {Structure.FOR_CONCEPT_RECOGNITION, Structure.FOR_CONCEPT_RECOGNITION, Structure.NOT_FOR_ANNOTATION, Structure.NOT_FOR_ANNOTATION, Structure.NOT_FOR_ANNOTATION, Structure.NOT_FOR_ANNOTATION, Structure.NOT_FOR_ANNOTATION};
    private static final Structure STRUCTURE = new Structure(ITEMKEYS, RESOURCE_ID, WEIGHTS, ONTOIDS);
    // Constant
    private static final String nifId = "nlx_154697-11";
    private static final String Title = "Title";
    private static final String Description = "Description";
    private static final String Author = "Author";
    private static final String Link_to_original_video = "Video Link";
    private static final String Category = "Category";
    private static final String Date_published = "Date published";
    private static final String Runtime = "Runtime";
    private static final String ele_database = "Database";

    // constructors
    public NIVAccessTool() {
        super(NAME, RESOURCE_ID, STRUCTURE);
        try {
            this.getToolResource().setResourceURL(new URL(URL));
            this.getToolResource().setResourceDescription(DESCRIPTION);
            this.getToolResource().setResourceLogo(new URL(LOGO));
            this.getToolResource().setResourceElementURL(ELT_URL);
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

        return ELT_URL + elementLocalID;
    }

    @Override
    public String mainContextDescriptor() {
        String MAIN_ITEMKEY = "Title";
        return MAIN_ITEMKEY;
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
                // populates OBR_NIV_ET with each of these experiments.
                myExp = i.next();
                try {
                    if (!myExp.getElementStructure().hasNullValues()) {
                        if (this.resourceUpdateService.addElement(myExp)) {
                            nbElement++;
                        }
                    }
                } catch (Exception e) {
                    logger.error("** PROBLEM ** Problem with id " + myExp.getLocalElementId() + " when populating the OBR_NIV_ET table.", e);
                }
            }
        } catch (Exception e) {
            logger.error("** PROBLEM ** Cannot update resource " + this.getToolResource().getResourceName(), e);
        }
        logger.info(nbElement + " elements added to the OBR_NIV_ET table.");
        return nbElement;
    }

    /**
     * get all Elements.
     */
    private HashSet<Element> getAllElements() {
        logger.info("* Get All Elements for NIF Integrated Videos... ");
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
                    Node tableData = dom.getFirstChild().getChildNodes().item(2);
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
                            if (name.equalsIgnoreCase(Title)) {
                                elementAttributes.put(Structure.generateContextName(RESOURCE_ID, ITEMKEYS[0]), value);
                            } else if (name.equalsIgnoreCase(Description)) {
                                elementAttributes.put(Structure.generateContextName(RESOURCE_ID, ITEMKEYS[1]), value);
                            } else if (name.equalsIgnoreCase(Author)) {
                                elementAttributes.put(Structure.generateContextName(RESOURCE_ID, ITEMKEYS[2]), value);
                            } else if (name.equalsIgnoreCase(Link_to_original_video)) {
                                if (value.length() > 0 && value.indexOf("href=\"http") > 0) {
                                    localElementId = value.substring(value.indexOf("http"), value.indexOf(endTag));
                                } else if (value.length() > 0 && value.indexOf("href=\"video") > 0) {
                                    localElementId = value.substring(value.indexOf("http"), value.lastIndexOf(endTag));
                                }
                            } else if (name.equalsIgnoreCase(Category)) {
                                elementAttributes.put(Structure.generateContextName(RESOURCE_ID, ITEMKEYS[4]), Jsoup.parse(value).text());
                            } else if (name.equalsIgnoreCase(Date_published)) {
                                elementAttributes.put(Structure.generateContextName(RESOURCE_ID, ITEMKEYS[5]), value);
                            } else if (name.equalsIgnoreCase(Runtime)) {
                                elementAttributes.put(Structure.generateContextName(RESOURCE_ID, ITEMKEYS[6]), value);
                            } else if (name.equalsIgnoreCase(ele_database)) {
                                elementAttributes.put(Structure.generateContextName(RESOURCE_ID, ITEMKEYS[3]), Jsoup.parse(value).text());
                            }
                        }

                        //Check if elementId is present locally.
                        if (allElementsInET.contains(localElementId)) {
                        } else {
                            if (localElementId.length() > 0) {
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
                Map<String, String> elementAttributes;
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

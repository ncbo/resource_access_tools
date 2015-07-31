/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.ncbo.resource_access_tools.resource.nif.antibodyregistry;

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
 * AccessTool for NIF Antibody Registry.
 * @author s.kharat
 */
public class ABRAccessTool extends AbstractNifResourceAccessTool {

    private static final String URL = "http://antibodyregistry.org/";
    private static final String NAME = "Antibody Registry (via NIF)";
    private static final String RESOURCEID = "ABR";
    private static final String DESCRIPTION = "The Antibody Registry aims to serve the scientific community by providing a very large list of antibodies (>900,000) as well as a set of unique identifiers for commercial and non-commercial antibody reagents allowing scientists to more easily keep track of data generated using a specific antibody. Sourcing and availability information are based on a semantic annotation pipeline supported by the Domeo web annotation toolkit, http://annotationframework.org/. "
            + "This public registry contains antibody product information organized according to genes, species, reagent types (antibodies, recombinant proteins, ELISA, siRNA, cDNA clones). Data is provided in many formats so that authors of biological papers, text mining tools and funding agencies can quickly and accurately identify the antibody reagents they and their colleagues used. "
            + "The Antibody Registry allows any user to submit a new antibody or set of antibodies to the registry via a web form, or via a spreadsheet upload.";
    private static final String LOGO = "http://neurolex.org/w/images/f/fe/BA45EBE0-E863-4A48-8495-91C8D4DAF22E.jpg";
    private static final String ELT_URL = "http://antibodyregistry.org/";
    private static final String[] ITEMKEYS = {"Antibody_Name", "Antibody_Target", "Source_Organsim", "Antibody_ID", "Vendor", "Cat_Num", "Clonality", "Clone_ID", "Comments"};
    private static final Double[] WEIGHTS = {1.0, 0.9, 0.8, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0};
    private static final String[] ONTOIDS = {Structure.FOR_CONCEPT_RECOGNITION, Structure.FOR_CONCEPT_RECOGNITION, Structure.FOR_CONCEPT_RECOGNITION, Structure.NOT_FOR_ANNOTATION,
        Structure.NOT_FOR_ANNOTATION, Structure.NOT_FOR_ANNOTATION, Structure.NOT_FOR_ANNOTATION, Structure.NOT_FOR_ANNOTATION, Structure.NOT_FOR_ANNOTATION};
    private static Structure STRUCTURE = new Structure(ITEMKEYS, RESOURCEID, WEIGHTS, ONTOIDS);
    private static String MAIN_ITEMKEY = "Antibody_Name";

    // Constant
    private static final String nifId = "nif-0000-07730-1";

    private static final String Antibody_Name = "Antibody Name";
    private static final String Antibody_Target = "Antibody Target";
    private static final String Source_Organism = "Source Organism";
    private static final String Antibody_ID = "Antibody ID";
    private static final String Vendor = "Vendor";
    private static final String Cat_Num = "Cat Num";
    private static final String Clonality = "Clonality";
    private static final String Clone_ID = "Clone ID";
    private static final String Comments = "Comments";
    private Map<String, String> localOntologyIDMap;

    // constructors
    public ABRAccessTool() {
        super(NAME, RESOURCEID, STRUCTURE);
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
        return ResourceType.MEDIUM;
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
                myExp = i.next();
                try {
                    if (!myExp.getElementStructure().hasNullValues()) {
                        if (this.resourceUpdateService.addElement(myExp)) {
                            nbElement++;
                        }
                    }
                } catch (Exception e) {
                    logger.error("** PROBLEM ** Problem with id " + myExp.getLocalElementId() + " when populating the OBR_ABR_ET table.", e);
                }
            }
        } catch (Exception e) {
            logger.error("** PROBLEM ** Cannot update resource " + this.getToolResource().getResourceName(), e);
        }
        logger.info(nbElement + " elements added to the OBR_ABR_ET table.");
        return nbElement;
    }

    /** This method is used to get all elements from resource site.
     *  @return HashSet<Element>
     */
    public HashSet<Element> getAllElements() {
        logger.info("* Get All Elements for Antibody Registry ... ");
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
                            if (name.equalsIgnoreCase(Antibody_Name)) {
                                elementAttributes.put(Structure.generateContextName(RESOURCEID, ITEMKEYS[0]), value);
                            } else if (name.equalsIgnoreCase(Antibody_Target)) {
                                elementAttributes.put(Structure.generateContextName(RESOURCEID, ITEMKEYS[1]), Jsoup.parse(value).text());
                            } else if (name.equalsIgnoreCase(Source_Organism)) {
                                elementAttributes.put(Structure.generateContextName(RESOURCEID, ITEMKEYS[2]), value);
                            } else if (name.equalsIgnoreCase(Antibody_ID)) {
                                localElementId = value.substring(value.indexOf(ELT_URL) + ELT_URL.length(), value.indexOf(endTag));
                                elementAttributes.put(Structure.generateContextName(RESOURCEID, ITEMKEYS[3]), Jsoup.parse(value).text());
                            } else if (name.equalsIgnoreCase(Vendor)) {
                                elementAttributes.put(Structure.generateContextName(RESOURCEID, ITEMKEYS[4]), value);
                            } else if (name.equalsIgnoreCase(Cat_Num)) {
                                elementAttributes.put(Structure.generateContextName(RESOURCEID, ITEMKEYS[5]), value);
                            } else if (name.equalsIgnoreCase(Clonality)) {
                                elementAttributes.put(Structure.generateContextName(RESOURCEID, ITEMKEYS[6]), value);
                            } else if (name.equalsIgnoreCase(Clone_ID)) {
                                elementAttributes.put(Structure.generateContextName(RESOURCEID, ITEMKEYS[7]), value);
                            } else if (name.equalsIgnoreCase(Comments)) {
                                elementAttributes.put(Structure.generateContextName(RESOURCEID, ITEMKEYS[8]), value);
                            }
                        }

                        //Check if elementId is present in database.
                        if (allElementsInET.contains(localElementId)) {
                            continue;
                        } else {
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

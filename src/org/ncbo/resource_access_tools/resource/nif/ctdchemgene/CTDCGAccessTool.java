/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.ncbo.resource_access_tools.resource.nif.ctdchemgene;

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
 *
 * @author s.kharat
 */
public class CTDCGAccessTool extends AbstractNifResourceAccessTool {

    private static final String URL = "http://ctdbase.org/";
    private static final String NAME = "CTD ChemGene (via NIF)";
    private static final String RESOURCEID = "CTDCG";
    private static final String DESCRIPTION = "A public database that enhances understanding about the effects of environmental chemicals on human health. "
            + "In detail, it contains information about gene/protein-disease associations, chemical-disease associations, interactions between chemicals and genes/proteins, "
            + "as well as the related pathways.";
    private static final String LOGO = "http://neurolex.org/w/images/b/bb/CTD.PNG";
    private static final String ELT_URL = "http://ctdbase.org/detail.go?type=gene&acc=";
    private static final String[] ITEMKEYS = {"Gene_Symbol", "Chemical_Name", "Gene_Forms", "Organism", "Interaction", "Type_of_Interactions", "References"};
    private static final Double[] WEIGHTS = {1.0, 0.9, 0.5, 0.7, 0.9, 0.8, 0.0};
    private static final String[] ONTOIDS = {Structure.FOR_CONCEPT_RECOGNITION, Structure.FOR_CONCEPT_RECOGNITION, Structure.FOR_CONCEPT_RECOGNITION, Structure.FOR_CONCEPT_RECOGNITION,
        Structure.FOR_CONCEPT_RECOGNITION, Structure.FOR_CONCEPT_RECOGNITION, Structure.NOT_FOR_ANNOTATION};
    private static Structure STRUCTURE = new Structure(ITEMKEYS, RESOURCEID, WEIGHTS, ONTOIDS);
    private static String MAIN_ITEMKEY = "Gene_Symbol";
    // Constant
    private static final String nifId = "nif-0000-02683-1";
    private static final String GeneSymbol = "Gene Symbol";
    private static final String ChemicalName = "Chemical Name";
    private static final String GeneForms = "Gene Forms";
    private static final String Organism = "Organism";
    private static final String Interaction = "Interaction";
    private static final String Types_of_Interaction = "Type of Interactions";
    private static final String References = "References";
    private Map<String, String> localOntologyIDMap;

    // constructors
    public CTDCGAccessTool() {
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
                    logger.error("** PROBLEM ** Problem with id " + myExp.getLocalElementId() + " when populating the OBR_CTDCG_ET table.", e);
                }
            }
        } catch (Exception e) {
            logger.error("** PROBLEM ** Cannot update resource " + this.getToolResource().getResourceName(), e);
        }
        logger.info(nbElement + " elements added to the OBR_CTDCG_ET table.");
        return nbElement;
    }

    /** This method is used to get all elements from resource site.
     *  @return HashSet<Element>
     */
    public HashSet<Element> getAllElements() {
        logger.info("* Get All Elements for NIF CTD ChemGene ... ");
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
                            if (name.equalsIgnoreCase(GeneSymbol)) {
                                localElementId = value.substring(value.indexOf(ELT_URL) + ELT_URL.length(), value.indexOf(endTag));
                                elementAttributes.put(Structure.generateContextName(RESOURCEID, ITEMKEYS[0]), Jsoup.parse(value).text());
                            } else if (name.equalsIgnoreCase(ChemicalName)) {
                                elementAttributes.put(Structure.generateContextName(RESOURCEID, ITEMKEYS[1]), Jsoup.parse(value).text());
                            } else if (name.equalsIgnoreCase(GeneForms)) {
                                elementAttributes.put(Structure.generateContextName(RESOURCEID, ITEMKEYS[2]), value);
                            } else if (name.equalsIgnoreCase(Organism)) {
                                elementAttributes.put(Structure.generateContextName(RESOURCEID, ITEMKEYS[3]), Jsoup.parse(value).text());
                            } else if (name.equalsIgnoreCase(Interaction)) {
                                elementAttributes.put(Structure.generateContextName(RESOURCEID, ITEMKEYS[4]), value);
                            } else if (name.equalsIgnoreCase(Types_of_Interaction)) {
                                elementAttributes.put(Structure.generateContextName(RESOURCEID, ITEMKEYS[5]), value);
                            } else if (name.equalsIgnoreCase(References)) {
                                elementAttributes.put(Structure.generateContextName(RESOURCEID, ITEMKEYS[6]), Jsoup.parse(value).text());
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

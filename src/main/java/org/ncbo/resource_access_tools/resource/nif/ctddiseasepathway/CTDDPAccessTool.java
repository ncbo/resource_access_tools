/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.ncbo.resource_access_tools.resource.nif.ctddiseasepathway;

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
 * AccessTool for CTD DiseasePathways (via NIF).
 *
 * @author s.kharat
 */
public class CTDDPAccessTool extends AbstractNifResourceAccessTool {

    private static final String URL = "http://ctdbase.org/";
    private static final String NAME = "CTD DiseasePathway (via NIF)";
    private static final String RESOURCEID = "CTDDP";
    private static final String DESCRIPTION = "A public database that enhances understanding about the effects of environmental chemicals on human health. "
            + "In detail, it contains information about gene/protein-disease associations, chemical-disease associations, interactions between chemicals and genes/proteins, "
            + "as well as the related pathways.";
    private static final String LOGO = "http://neurolex.org/w/images/b/bb/CTD.PNG";
    private static final String ELT_URL = "http://ctdbase.org/detail.go?type=disease&acc=";
    private static final String[] ITEMKEYS = {"Pathway_Name", "Disease_Name", "Inference_Gene_Symbol"};
    private static final Double[] WEIGHTS = {1.0, 0.9, 0.9};
    private static final String[] ONTOIDS = {Structure.FOR_CONCEPT_RECOGNITION, Structure.FOR_CONCEPT_RECOGNITION, Structure.FOR_CONCEPT_RECOGNITION};
    private static final Structure STRUCTURE = new Structure(ITEMKEYS, RESOURCEID, WEIGHTS, ONTOIDS);
    // Constant
    private static final String nifId = "nif-0000-02683-3";
    private static final String Pathway_Name = "Pathway Name";
    private static final String DiseaseName = "Disease";
    private static final String Inference_Gene_Symbol = "Association Inferred via Gene";

    // constructors
    public CTDDPAccessTool() {
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
        String MAIN_ITEMKEY = "Disease_Name";
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
                    logger.error("** PROBLEM ** Problem with id " + myExp.getLocalElementId() + " when populating the OBR_CTDDP_ET table.", e);
                }
            }
        } catch (Exception e) {
            logger.error("** PROBLEM ** Cannot update resource " + this.getToolResource().getResourceName(), e);
        }
        logger.info(nbElement + " elements added to the OBR_CTDDP_ET table.");
        return nbElement;
    }

    /**
     * This method is used to get all elements from resource site.
     *
     * @return HashSet<Element>
     */
    private HashSet<Element> getAllElements() {
        logger.info("* Get All Elements for NIF CTD DiseasePathways ... ");
        HashSet<Element> elementSet = new HashSet<Element>();
        int nbAdded;
        int offset = 0;
        int totalCount = 0;

        try {

            HashSet<String> allElementsInET = this.resourceUpdateService.getAllLocalElementIDs();

            Map<StringBuffer, Map<String, String>> allRowsData = new HashMap<StringBuffer, Map<String, String>>();

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
                        StringBuffer localElementId = new StringBuffer(EMPTY_STRING);
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
                            if (name.equalsIgnoreCase(Pathway_Name)) {
                                elementAttributes.put(Structure.generateContextName(RESOURCEID, ITEMKEYS[0]), value);
                            } else if (name.equalsIgnoreCase(DiseaseName)) {
                                localElementId = new StringBuffer(value.substring(value.indexOf(ELT_URL) + ELT_URL.length(), value.indexOf(endTag)));
                                elementAttributes.put(Structure.generateContextName(RESOURCEID, ITEMKEYS[1]), Jsoup.parse(value).text());
                            } else if (name.equalsIgnoreCase(Inference_Gene_Symbol)) {
                                elementAttributes.put(Structure.generateContextName(RESOURCEID, ITEMKEYS[2]), value);
                            }
                        }

                        //Check if elementId is present in database.  if (allElementsInET.contains(localElementId + PathwayN + DiseaseN + InferenceGS)) {
                        if (allElementsInET.contains(localElementId)) {
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
            for (StringBuffer localElementID : allRowsData.keySet()) {
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
                    Element exp = new Element(localElementID.toString(), elementStructure);
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

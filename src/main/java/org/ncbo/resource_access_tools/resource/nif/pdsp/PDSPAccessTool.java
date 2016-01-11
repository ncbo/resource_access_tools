/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.ncbo.resource_access_tools.resource.nif.pdsp;

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
 * AccessTool for NIF PDSP
 *
 * @author s.kharat
 */
public class PDSPAccessTool extends AbstractNifResourceAccessTool {

    private static final String PDSP_URL = "http://pdsp.med.unc.edu/pdsp.php";
    private static final String PDSP_NAME = "PDSP Ki database (via NIF)";
    private static final String PDSP_RESOURCEID = "PDSP";
    private static final String PDSP_DESCRIPTION = "The PDSP Ki database is a unique resource in the public domain which provides information on the abilities of drugs to interact with an expanding number of molecular targets. "
            + "The Ki database serves as a data warehouse for published and internally-derived Ki, or affinity, values for a large number of drugs and drug candidates at an expanding number of G-protein coupled receptors, ion channels, transporters and enzymes. "
            + "The query interface is designed to let you search by any field, or combination of them to refine your search criteria. The flexible user interface also provides for customized data mining.";
    private static final String PDSP_LOGO = "http://neurolex.org/w/images/a/a3/PDSPKI.gif";
    private static final String PDSP_ELT_URL = "http://pdsp.med.unc.edu/pdsp.php?knowID=retreive+this+value+only&kiKey=";
    private static final String PDSP_ELT_URL_1 = "&receptorDD=&receptor=&speciesDD=&species=&sourcesDD=&source=&hotLigandDD=&hotLigand=&testLigandDD=&testLigand=&referenceDD=&reference=&KiGreater=&KiLess=&kiAllRadio=all";
    private static final String[] PDSP_ITEMKEYS = {"Receptor", "Ligand", "Hotligand", "Organism", "Structure", "unit_nM"};
    private static final Double[] PDSP_WEIGHTS = {1.0, 0.9, 0.5, 0.9, 0.9, 0.5};
    private static final String[] PDSP_ONTOIDS = {Structure.FOR_CONCEPT_RECOGNITION, Structure.FOR_CONCEPT_RECOGNITION, Structure.FOR_CONCEPT_RECOGNITION, Structure.FOR_CONCEPT_RECOGNITION, Structure.FOR_CONCEPT_RECOGNITION, Structure.NOT_FOR_ANNOTATION};
    private static final Structure PDSP_STRUCTURE = new Structure(PDSP_ITEMKEYS, PDSP_RESOURCEID, PDSP_WEIGHTS, PDSP_ONTOIDS);
    // Constant
    private static final String nifId = "nif-0000-01866-1";
    private static final String PDSP_SearchKi = "Search Ki";
    private static final String PDSP_Receptor = "Receptor";
    private static final String PDSP_Ligand = "Ligand";
    private static final String PDSP_HotLigand = "Hot Ligand";
    private static final String PDSP_Organism = "Organism";
    private static final String PDSP_Struct = "Structure";
    private static final String PDSP_Ki = "ki(nM)";

    // constructors
    public PDSPAccessTool() {
        super(PDSP_NAME, PDSP_RESOURCEID, PDSP_STRUCTURE);
        try {
            this.getToolResource().setResourceURL(new URL(PDSP_URL));
            this.getToolResource().setResourceDescription(PDSP_DESCRIPTION);
            this.getToolResource().setResourceLogo(new URL(PDSP_LOGO));
            this.getToolResource().setResourceElementURL(PDSP_ELT_URL);
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
        return PDSP_ELT_URL + elementLocalID + PDSP_ELT_URL_1;
    }

    @Override
    public String mainContextDescriptor() {
        String PDSP_MAIN_ITEMKEY = "Receptor";
        return PDSP_MAIN_ITEMKEY;
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
                // populates OBR_MDB_ET with each of these experiments.
                myExp = i.next();
                try {
                    if (!myExp.getElementStructure().hasNullValues()) {
                        if (this.resourceUpdateService.addElement(myExp)) {
                            nbElement++;
                        }
                    }
                } catch (Exception e) {
                    logger.error("** PROBLEM ** Problem with id " + myExp.getLocalElementId() + " when populating the OBR_PDSP_ET table.", e);
                }
            }
        } catch (Exception e) {
            logger.error("** PROBLEM ** Cannot update resource " + this.getToolResource().getResourceName(), e);
        }
        logger.info(nbElement + " elements added to the OBR_PDSP_ET table.");
        return nbElement;
    }

    /**
     * get all Elements.
     */
    private HashSet<Element> getAllElements() {
        logger.info("* Get All Elements for PDSP ... ");
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
                            if (name.equalsIgnoreCase(PDSP_SearchKi)) {         //localElementId
                                localElementId = Jsoup.parse(value).text();
                            } else if (name.equalsIgnoreCase(PDSP_Receptor)) {  //Receptor
                                elementAttributes.put(Structure.generateContextName(PDSP_RESOURCEID, PDSP_ITEMKEYS[0]), value);
                            } else if (name.equalsIgnoreCase(PDSP_Ligand)) {    //Ligand
                                elementAttributes.put(Structure.generateContextName(PDSP_RESOURCEID, PDSP_ITEMKEYS[1]), value);
                            } else if (name.equalsIgnoreCase(PDSP_HotLigand)) { //HotLigand
                                elementAttributes.put(Structure.generateContextName(PDSP_RESOURCEID, PDSP_ITEMKEYS[2]), value);
                            } else if (name.equalsIgnoreCase(PDSP_Organism)) {  //Organism
                                elementAttributes.put(Structure.generateContextName(PDSP_RESOURCEID, PDSP_ITEMKEYS[3]), value);
                            } else if (name.equalsIgnoreCase(PDSP_Struct)) {    //Structure
                                elementAttributes.put(Structure.generateContextName(PDSP_RESOURCEID, PDSP_ITEMKEYS[4]), value);
                            } else if (name.equalsIgnoreCase(PDSP_Ki)) {        //ki_nM
                                elementAttributes.put(Structure.generateContextName(PDSP_RESOURCEID, PDSP_ITEMKEYS[5]), value);
                            }
                        }

                        //Check if elementId is present locally.
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
            for (String localElementID : allRowsData.keySet()) {
                Map<String, String> elementAttributes = new HashMap<String, String>();
                elementAttributes = allRowsData.get(localElementID);

                // PUT DATA INTO A STRUCTURE++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
                Structure elementStructure = new Structure(PDSP_STRUCTURE.getContextNames());
                for (String contextName : PDSP_STRUCTURE.getContextNames()) {
                    boolean attributeHasValue = false;

                    for (String att : elementAttributes.keySet()) {
                        if (contextName.equals(att)) {
                            // not an existing annotation
                            if (PDSP_STRUCTURE.getOntoID(contextName).equals(Structure.FOR_CONCEPT_RECOGNITION)
                                    || PDSP_STRUCTURE.getOntoID(contextName).equals(Structure.NOT_FOR_ANNOTATION)) {
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

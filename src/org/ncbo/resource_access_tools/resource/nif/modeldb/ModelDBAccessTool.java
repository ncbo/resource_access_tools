/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.ncbo.resource_access_tools.resource.nif.modeldb;

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
 * AccessTool for NIF ModelDB
 * @author s.kharat
 */
public class ModelDBAccessTool extends AbstractNifResourceAccessTool {

    private static final String MDB_URL = "http://senselab.med.yale.edu/ModelDB/";
    private static final String MDB_NAME = "ModelDB (via NIF)";
    private static final String MDB_RESOURCEID = "MDB";
    private static final String MDB_DESCRIPTION = "ModelDB provides an accessible location for storing and efficiently retrieving computational neuroscience models. ModelDB is tightly coupled with NeuronDB. Models can be coded in any language for any environment. Model code can be viewed before downloading and browsers can be set to auto-launch the models. "
            + "ModelDB is a curated database of published models in the broad domain of computational neuroscience. It addresses the need for access to such models in order to evaluate their validity and extend their use. It can handle computational models expressed in any textual form, including procedural or declarative languages (e.g. C , XML dialects) and source code written for any simulation environment. The model source code doesn't even have to reside inside ModelDB; it just has to be available from some publicly accessible online repository or WWW site. "
            + "ModelDB is curated in order to maximize the scientific utility of its contents. The ideal model entry would contain \"original\" (author-written) source code, especially if it works and reproduces at least one figure from a published article. Original source code has tremendous value because it is what the authors used to generate the simulation results from which they derived their published insights and conclusions. High quality \"third party\" re-implementations of published models are also relevant, especially those involving models that are of wide interest.";
    private static final String MDB_LOGO = "http://neurolex.org/w/images/7/74/Modeldb.png";
    private static final String MDB_ELT_URL = "http://senselab.med.yale.edu/ModelDB/ShowModel.asp?model=";
    private static final String[] MDB_ITEMKEYS = {"title", "modelTypes", "cellTypes", "channels", "receptors", "transmitters"};
    private static final Double[] MDB_WEIGHTS = {1.0, 1.0, 1.0, 0.8, 0.8, 0.8};
    private static final String[] MDB_ONTOIDS = {Structure.FOR_CONCEPT_RECOGNITION, Structure.FOR_CONCEPT_RECOGNITION, Structure.FOR_CONCEPT_RECOGNITION, Structure.FOR_CONCEPT_RECOGNITION, Structure.FOR_CONCEPT_RECOGNITION, Structure.FOR_CONCEPT_RECOGNITION};
    private static Structure MDB_STRUCTURE = new Structure(MDB_ITEMKEYS, MDB_RESOURCEID, MDB_WEIGHTS, MDB_ONTOIDS);
    private static String MDB_MAIN_ITEMKEY = "title";
    // Constants
    private static final String nifId = "nif-0000-00004-1";
    private static final String title = "Model Name";
    private static final String modelTypes = "Type";
    private static final String cellTypes = "Neurons";
    private static final String channels = "Currents";
    private static final String receptors = "Receptors";
    private static final String transmitters = "Neurotransmitters";
    private Map<String, String> localOntologyIDMap;

    // constructor
    public ModelDBAccessTool() {
        super(MDB_NAME, MDB_RESOURCEID, MDB_STRUCTURE);
        try {
            this.getToolResource().setResourceURL(new URL(MDB_URL));
            this.getToolResource().setResourceDescription(MDB_DESCRIPTION);
            this.getToolResource().setResourceLogo(new URL(MDB_LOGO));
            this.getToolResource().setResourceElementURL(MDB_ELT_URL);
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
        return MDB_ELT_URL + elementLocalID;
    }

    @Override
    public String mainContextDescriptor() {
        return MDB_MAIN_ITEMKEY;
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
                    logger.error("** PROBLEM ** Problem with id " + myExp.getLocalElementId() + " when populating the OBR_MDB_ET table.", e);
                }
            }
        } catch (Exception e) {
            logger.error("** PROBLEM ** Cannot update resource " + this.getToolResource().getResourceName(), e);
        }
        logger.info(nbElement + " elements added to the OBR_MDB_ET table.");
        return nbElement;
    }

    /**
     * get all Elements.
     */
    public HashSet<Element> getAllElements() {
        logger.info("* Get All Elements for ModelDB ... ");
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
                            if (name.equalsIgnoreCase(title)) { //local Element Id and Tittle
                                localElementId = value.substring(value.indexOf(MDB_ELT_URL) + MDB_ELT_URL.length(), value.indexOf(endTag));
                                elementAttributes.put(Structure.generateContextName(MDB_RESOURCEID, MDB_ITEMKEYS[0]), Jsoup.parse(value).text());
                            } else if (name.equalsIgnoreCase(modelTypes)) { //ModelType
                                elementAttributes.put(Structure.generateContextName(MDB_RESOURCEID, MDB_ITEMKEYS[1]), value);
                            } else if (name.equalsIgnoreCase(cellTypes)) { //cellType
                                elementAttributes.put(Structure.generateContextName(MDB_RESOURCEID, MDB_ITEMKEYS[2]), value);
                            } else if (name.equalsIgnoreCase(channels)) { //Channels
                                elementAttributes.put(Structure.generateContextName(MDB_RESOURCEID, MDB_ITEMKEYS[3]), value);
                            } else if (name.equalsIgnoreCase(receptors)) { //Receptors
                                elementAttributes.put(Structure.generateContextName(MDB_RESOURCEID, MDB_ITEMKEYS[4]), value);
                            } else if (name.equalsIgnoreCase(transmitters)) { //Transmitters
                                elementAttributes.put(Structure.generateContextName(MDB_RESOURCEID, MDB_ITEMKEYS[5]), value);
                            }
                            //Check if elementId is present locally.
                            if (allElementsInET.contains(localElementId)) {
                                continue;
                            } else {
                                allRowsData.put(localElementId, elementAttributes);
                            }
                        }
                    }
                } else {
                    offset += rowCount;
                    logger.info("Increase OFFSET");
                }
            } while (offset < totalCount);

            //parsing ends

            // Second phase: creation of elements
            for (String localElementID : allRowsData.keySet()) {
                Map<String, String> elementAttributes = new HashMap<String, String>();
                elementAttributes = allRowsData.get(localElementID);

                // PUT DATA INTO A STRUCTURE++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
                Structure elementStructure = new Structure(MDB_STRUCTURE.getContextNames());
                for (String contextName : MDB_STRUCTURE.getContextNames()) {
                    boolean attributeHasValue = false;

                    for (String att : elementAttributes.keySet()) {
                        if (contextName.equals(att)) {
                            // not an existing annotation
                            if (MDB_STRUCTURE.getOntoID(contextName).equals(Structure.FOR_CONCEPT_RECOGNITION)
                                    || MDB_STRUCTURE.getOntoID(contextName).equals(Structure.NOT_FOR_ANNOTATION)) {
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

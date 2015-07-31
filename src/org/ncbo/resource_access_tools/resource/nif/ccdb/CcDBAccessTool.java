/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.ncbo.resource_access_tools.resource.nif.ccdb;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;

import org.ncbo.resource_access_tools.enumeration.ResourceType;
import org.ncbo.resource_access_tools.populate.Element;
import org.ncbo.resource_access_tools.populate.Structure;
import org.ncbo.resource_access_tools.resource.nif.AbstractNifResourceAccessTool;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * AccessTool for NIF CCDB
 * @author s.kharat
 */
public class CcDBAccessTool extends AbstractNifResourceAccessTool {

    private static final String CCDB_URL = "http://ccdb.ucsd.edu";
    private static final String CCDB_NAME = "Cell Centered Database (via NIF)";
    private static final String CCDB_RESOURCEID = "CCDB";
    private static final String CCDB_DESCRIPTION = "The Cell Centered Database is a publicly accessible resource for high resolution 2D, 3D and 4D data from light and electron microscopy, including correlated imaging. Techniques range from wide field mosaics taken with multiphoton microscopy to 3D reconstructions of cellular ultrastructure using electron tomography. Contributions from the community are welcome. "
            + "The CCDB was designed around the process of reconstruction from 2D micrographs, capturing key steps in the process from experiment to analysis. The CCDB refers to the set of images taken from the as the Microscopy Product. The microscopy product refers to a set of related 2D images taken by light (epifluorescence, transmitted light, confocal or multiphoton) or electron microscopy (conventional or high voltage transmission electron microscopy). "
            + "These image sets may comprise a tilt series, optical section series, through focus series, serial sections, mosaics, time series or a set of survey sections taken in a single microscopy session that are not related in any systematic way. A given set of data may be more than one product, for example, it is possible for a set of images to be both a mosaic and a tilt series. The Microscopy Product ID serves as the accession number for the CCDB. "
            + "All microscopy products must belong to a project and be stored along with key specimen preparation details. Each project receives a unique Project ID that groups together related microscopy products. Many of the datasets come from published literature, but publication is not a prerequisite for inclusion in the CCDB. Any datasets that are of high quality and interest to the scientific community can be included in the CCDB. "
            + "The goal of the CCDB project is to make unique and valuable datasets available to the scientific community for visualization, reuse and reanalysis. Data in the CCDB can be accessed by performing a Search or by browsing through our Gallery. Data in the CCDB may be downloaded freely and reused for non-profit use within the terms of our usage agreement.";
    private static final String CCDB_LOGO = "http://neurolex.org/w/images/a/a4/Ccdb.jpg";
    private static final String CCDB_ELT_URL = "http://ccdb.ucsd.edu/sand/main?event=displaySum&mpid=";
    private static final String[] CCDB_ITEMKEYS = {"ProjectName", "Species", "Region", "CellType", "Age", "Microscope_Type"};
    private static final Double[] CCDB_WEIGHTS = {1.0, 0.8, 0.9, 0.9, 0.5, 0.5}; //, 0.6, 0.8
    private static final String[] CCDB_ONTOIDS = {Structure.FOR_CONCEPT_RECOGNITION, Structure.FOR_CONCEPT_RECOGNITION, Structure.FOR_CONCEPT_RECOGNITION, Structure.FOR_CONCEPT_RECOGNITION, Structure.NOT_FOR_ANNOTATION, Structure.NOT_FOR_ANNOTATION}; //, Structure.FOR_CONCEPT_RECOGNITION, Structure.FOR_CONCEPT_RECOGNITION
    private static Structure CCDB_STRUCTURE = new Structure(CCDB_ITEMKEYS, CCDB_RESOURCEID, CCDB_WEIGHTS, CCDB_ONTOIDS);
    private static String CCDB_MAIN_ITEMKEY = "ProjectName";
    // Constant
    private static final String nifId = "nif-0000-00007-1";
    private static final String CCDB_ProjectName = "Project Name";
    private static final String CCDB_Species = "Organism";
    private static final String CCDB_Region = "Brain Region";
    private static final String CCDB_CellTypes = "Cell Type";
    private static final String CCDB_Image = "Image"; //Required to retrive localElementId
    private static final String CCDB_Age = "Age";
    private static final String CCDB_MicroscopeType = "Microscope Type";
    private Map<String, String> localOntologyIDMap;

    // constructors
    public CcDBAccessTool() {
        super(CCDB_NAME, CCDB_RESOURCEID, CCDB_STRUCTURE);
        try {
            this.getToolResource().setResourceURL(new URL(CCDB_URL));
            this.getToolResource().setResourceDescription(CCDB_DESCRIPTION);
            this.getToolResource().setResourceLogo(new URL(CCDB_LOGO));
            this.getToolResource().setResourceElementURL(CCDB_ELT_URL);
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
        return CCDB_ELT_URL + elementLocalID;
    }

    @Override
    public String mainContextDescriptor() {
        return CCDB_MAIN_ITEMKEY;
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
                    logger.error("** PROBLEM ** Problem with id " + myExp.getLocalElementId() + " when populating the OBR_CCDB_ET table.", e);
                }
            }
        } catch (Exception e) {
            logger.error("** PROBLEM ** Cannot update resource " + this.getToolResource().getResourceName(), e);
        }
        logger.info(nbElement + " elements added to the OBR_CCDB_ET table.");
        return nbElement;
    }

    /**
     * get all Elements.
     */
    public HashSet<Element> getAllElements() {
        logger.info("* Get All Elements for CCDB ... ");
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
                            if (name.equalsIgnoreCase(CCDB_Image)) {                //localElementId
                                localElementId = value.substring(value.indexOf(CCDB_ELT_URL) + CCDB_ELT_URL.length(), value.indexOf(endTag));
                            } else if (name.equalsIgnoreCase(CCDB_ProjectName)) {   //ProjectName
                                elementAttributes.put(Structure.generateContextName(CCDB_RESOURCEID, CCDB_ITEMKEYS[0]), value);
                            } else if (name.equalsIgnoreCase(CCDB_Species)) {       //Species
                                elementAttributes.put(Structure.generateContextName(CCDB_RESOURCEID, CCDB_ITEMKEYS[1]), value);
                            } else if (name.equalsIgnoreCase(CCDB_Region)) {        //Region
                                elementAttributes.put(Structure.generateContextName(CCDB_RESOURCEID, CCDB_ITEMKEYS[2]), value);
                            } else if (name.equalsIgnoreCase(CCDB_CellTypes)) {     //CellTypes
                                elementAttributes.put(Structure.generateContextName(CCDB_RESOURCEID, CCDB_ITEMKEYS[3]), value);
                            } else if (name.equalsIgnoreCase(CCDB_Age)) {           //Age
                                elementAttributes.put(Structure.generateContextName(CCDB_RESOURCEID, CCDB_ITEMKEYS[4]), value);
                            } else if (name.equalsIgnoreCase(CCDB_MicroscopeType)) {//Microscope Type
                                elementAttributes.put(Structure.generateContextName(CCDB_RESOURCEID, CCDB_ITEMKEYS[5]), value);
                            }
                        }
                        //appending localElementId to project Name.
                        elementAttributes.put(Structure.generateContextName(CCDB_RESOURCEID, CCDB_ITEMKEYS[0]), elementAttributes.get(Structure.generateContextName(CCDB_RESOURCEID, CCDB_ITEMKEYS[0])) + " " + localElementId);

                        //Check if elementId is present locally.
                        if (allElementsInET.contains(localElementId)) {
                            continue;
                        } else {
                            //adding more attribte retrived from second indexable request
                            if (allRowsData.containsKey(localElementId)) {
                                //   System.out.println("Updating previuos map for: " + localElementId);
                                Map<String, String> initEleAttributes = allRowsData.get(localElementId);
                                for (String key : elementAttributes.keySet()) {
                                    initEleAttributes.put(key, elementAttributes.get(key));
                                }
                                allRowsData.put(localElementId, initEleAttributes);
                            } else {
                                //    System.out.println("Adding value for: " + localElementId);
                                allRowsData.put(localElementId, elementAttributes);
                            }
                        }
                    }

                }
            } while (offset < totalCount);

            //parsing ends

            // Second phase: creation of elements
            for (String localElementID : allRowsData.keySet()) {
                Map<String, String> elementAttributes = new HashMap<String, String>();
                elementAttributes = allRowsData.get(localElementID);

                // PUT DATA INTO A STRUCTURE++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
                Structure elementStructure = new Structure(CCDB_STRUCTURE.getContextNames());
                for (String contextName : CCDB_STRUCTURE.getContextNames()) {
                    boolean attributeHasValue = false;

                    for (String att : elementAttributes.keySet()) {
                        if (contextName.equals(att)) {
                            // not an existing annotation
                            if (CCDB_STRUCTURE.getOntoID(contextName).equals(Structure.FOR_CONCEPT_RECOGNITION)
                                    || CCDB_STRUCTURE.getOntoID(contextName).equals(Structure.NOT_FOR_ANNOTATION)) {
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

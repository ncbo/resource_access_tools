/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.ncbo.resource_access_tools.resource.nif.drugrelatedgenedatabase;

import org.ncbo.resource_access_tools.resource.nif.AbstractNifResourceAccessTool;
import org.ncbo.resource_access_tools.resource.nif.AbstractNifResourceAccessTool;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import obs.obr.populate.Element;
import obs.obr.populate.Structure;
import org.jsoup.Jsoup;
import org.ncbo.stanford.obr.enumeration.ResourceType;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * AccessTool for  NIF Drug Related Gene Database.
 * @author s.kharat
 */
public class DRGDBAccessTool extends AbstractNifResourceAccessTool {

    private static final String URL = "https://confluence.crbs.ucsd.edu/display/NIF/DRG";
    private static final String NAME = "Drug Related Gene Database (via NIF)";
    private static final String RESOURCEID = "DRGDB";
    private static final String DESCRIPTION = "The Related Gene (DRG) Database gene expression data are extracted from published journal articles that "
            + "test hypotheses relevant to the neuroscience of addiction and addictive behavior. Data types include the effects of a particular drug, "
            + "strain, or knock out on a particular gene, in a particular anatomical region.";
    private static final String LOGO = "http://neurolex.org/w/images/a/ae/DRG.PNG";
    private static final String ELT_URL = "http://www.ncbi.nlm.nih.gov/pubmed/";
    private static final String[] ITEMKEYS = {"Gene_Name", "Treatment", "Brain_region", "Exp_vs_Control", "Publication","Probe_ID","Expression","Protocol_Type","Table"};
    private static final Double[] WEIGHTS = {1.0, 0.9, 0.9, 0.8, 0.0,0.0,0.0,0.0,0.0};
    private static final String[] ONTOIDS = {Structure.FOR_CONCEPT_RECOGNITION, Structure.FOR_CONCEPT_RECOGNITION, Structure.FOR_CONCEPT_RECOGNITION, Structure.FOR_CONCEPT_RECOGNITION,
        Structure.NOT_FOR_ANNOTATION, Structure.NOT_FOR_ANNOTATION, Structure.NOT_FOR_ANNOTATION, Structure.NOT_FOR_ANNOTATION, Structure.NOT_FOR_ANNOTATION};
    private static Structure STRUCTURE = new Structure(ITEMKEYS, RESOURCEID, WEIGHTS, ONTOIDS);
    private static String MAIN_ITEMKEY = "Gene_Name";

    // Constant
    private static final String nifId = "nif-0000-37443-1";
    private static final String Database = "Drug Related Gene Database";
    private static final String Indexable = "DRG";
    private static final String Publication = "Publication";
    private static final String Gene_Name = "Gene Name";
    private static final String Brain_Region = "Brain Region";
    private static final String Treatment = "Treatment";
    private static final String Exp_vs_Control = "Exp vs Control";
    private static final String Probe_ID = "Probe ID";
    private static final String Expression = "Expression";
    private static final String Protocol_Type = "Protocol Type";
    private static final String Table = "Table";

    private Map<String, String> localOntologyIDMap;

    // constructors
    public DRGDBAccessTool() {
        super(NAME, RESOURCEID, STRUCTURE);
        try {
            this.getToolResource().setResourceURL(new URL(URL));
            this.getToolResource().setResourceDescription(DESCRIPTION);
            this.getToolResource().setResourceLogo(new URL(LOGO));
            this.getToolResource().setResourceElementURL(ELT_URL);
        } catch (MalformedURLException e) {
            logger.error(EMPTY_STRING, e);
        }
        localOntologyIDMap = createLocalOntologyIDMap(STRUCTURE);
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
        //separating row count value from localElementId
        String elemetId []= elementLocalID.split(SLASH_STRING);
        return ELT_URL + elemetId[0];
    }

    @Override
    public String mainContextDescriptor() {
        return MAIN_ITEMKEY;
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
                myExp = i.next();
                try {
                    if (!myExp.getElementStructure().hasNullValues()) {
                        if (this.resourceUpdateService.addElement(myExp)) {
                            nbElement++;
                        }
                    }
                } catch (Exception e) {
                    logger.error("** PROBLEM ** Problem with id " + myExp.getLocalElementId() + " when populating the OBR_IDV_ET table.", e);
                }
            }
        } catch (Exception e) {
            logger.error("** PROBLEM ** Cannot update resource " + this.getToolResource().getResourceName(), e);
        }
        logger.info(nbElement + " elements added to the OBR_DRGDB_ET table.");
        return nbElement;
    }

    /** This method is used to get all elements from resource site.
     *  @return HashSet<Element>
     */
    public HashSet<Element> getAllElements() {
    	logger.info("* Get All Elements for Drug Related Gene Database ... ");
        HashSet<Element> elementSet = new HashSet<Element>();
        int nbAdded = 0;
        int offset = 0;
        int totalCount = 0;

        try {
            //get all elements from _ET table
            //Unique entry combination for this resource is checked against 3 fields (Publiaction + Gene name + Treatment)
            HashSet<String> allElementsInET  = this.resourceUpdateService.getAllValuesByColumn("concat(drgdb_publication,drgdb_gene_name,drgdb_treatment)");

            Map<String, Map<String, String>> allRowsData = new HashMap<String, Map<String, String>>();
            int rowcnt = 1;

            //parsing data
            do {
                //Document dom = queryFederation(Database, Indexable, query, offset, rowCount);
            	Document dom = queryFederation(nifId, query, offset, rowCount);

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
                        String gene = EMPTY_STRING;
                        String treatment = EMPTY_STRING;
                        String pub = EMPTY_STRING;
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
                            if (name.equalsIgnoreCase(Gene_Name)) {
                                gene = value;
                                elementAttributes.put(Structure.generateContextName(RESOURCEID, ITEMKEYS[0]), gene);
                            } else if (name.equalsIgnoreCase(Treatment)) {
                                treatment = Jsoup.parse(value).text();
                                elementAttributes.put(Structure.generateContextName(RESOURCEID, ITEMKEYS[1]), treatment);
                            } else if (name.equalsIgnoreCase(Brain_Region)) {
                                elementAttributes.put(Structure.generateContextName(RESOURCEID, ITEMKEYS[2]), value);
                            } else if (name.equalsIgnoreCase(Exp_vs_Control)) {
                                elementAttributes.put(Structure.generateContextName(RESOURCEID, ITEMKEYS[3]), value);
                            } else if (name.equalsIgnoreCase(Publication)) {
                                localElementId = value.substring(value.indexOf(ELT_URL) + ELT_URL.length(), value.indexOf(endTag));
                                pub =  Jsoup.parse(value).text();
                                elementAttributes.put(Structure.generateContextName(RESOURCEID, ITEMKEYS[4]), pub);
                            } else if (name.equalsIgnoreCase(Probe_ID)) {
                                elementAttributes.put(Structure.generateContextName(RESOURCEID, ITEMKEYS[5]), value);
                            } else if (name.equalsIgnoreCase(Expression)) {
                                elementAttributes.put(Structure.generateContextName(RESOURCEID, ITEMKEYS[6]), value);
                            } else if (name.equalsIgnoreCase(Protocol_Type)) {
                                elementAttributes.put(Structure.generateContextName(RESOURCEID, ITEMKEYS[7]), value);
                            } else if (name.equalsIgnoreCase(Table)) {
                                elementAttributes.put(Structure.generateContextName(RESOURCEID, ITEMKEYS[8]), Jsoup.parse(value).text());
                            }
                        }


                        //Check if elementId is present in database.
                        if (allElementsInET.contains(pub + gene + treatment)) {
                            continue;
                        } else {
                            allElementsInET.add(pub + gene + treatment);

                            //additional row count value appended to localElementId to overcome unique constraint restriction for this column in DB.
                            localElementId += SLASH_STRING + rowcnt;
                            rowcnt++;
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

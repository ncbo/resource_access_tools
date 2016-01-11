package org.ncbo.resource_access_tools.resource.arrayexpress;

import org.ncbo.resource_access_tools.enumeration.ResourceType;
import org.ncbo.resource_access_tools.populate.Element;
import org.ncbo.resource_access_tools.populate.Element.BadElementStructureException;
import org.ncbo.resource_access_tools.populate.Structure;
import org.ncbo.resource_access_tools.resource.AbstractXmlResourceAccessTool;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

public class ArrayExpressAccessTool extends AbstractXmlResourceAccessTool {

    private static final String AE_URL = "http://www.ebi.ac.uk/arrayexpress/";
    private static final String AE_NAME = "ArrayExpress";
    private static final String AE_RESOURCEID = "AE";
    private static final String AE_DESCRIPTION = "ArrayExpress is a public repository for microarray data, which is aimed at storing MIAME-compliant data in accordance with MGED recommendations. The ArrayExpress Data Warehouse stores gene-indexed expression profiles from a curated subset of experiments in the repository.";
    private static final String AE_LOGO = "http://www.ebi.ac.uk/microarray-as/aer/include/aelogo.png";
    private static final String AE_SERVICE = "http://www.ebi.ac.uk/arrayexpress/xml/v2/experiments";

    private static final String AE_ELT_URL = "http://www.ebi.ac.uk/arrayexpress/experiments/";

    private static final String[] AE_ITEMKEYS = {"name", "description", "species", "experiment_type"};
    private static final Double[] AE_WEIGHTS = {1.0, 0.8, 1.0, 0.9};

    // OntoID associated for reported annotations
    private static final String[] AE_ONTOIDS = {Structure.FOR_CONCEPT_RECOGNITION, Structure.FOR_CONCEPT_RECOGNITION, "1132", "1136"};

    private static final Structure AE_STRUCTURE = new Structure(AE_ITEMKEYS, AE_RESOURCEID, AE_WEIGHTS, AE_ONTOIDS);

    // Constant for 'experiment' string
    private static final String AE_EXPERIMENT = "experiment";
    private static final String AE_EXPERIMENT_TYPE = "experimenttype";
    private static final String ELT_ACCNUM = "accession";

    private static final String ELT_NAME = Structure.generateContextName(AE_RESOURCEID, AE_ITEMKEYS[0]);
    private static final String ELT_SPECIES = Structure.generateContextName(AE_RESOURCEID, AE_ITEMKEYS[2]);
    private static final String ELT_DESCRIPTION = Structure.generateContextName(AE_RESOURCEID, AE_ITEMKEYS[1]);
    private static final String ELT_EXPERIMENT_TYPE = Structure.generateContextName(AE_RESOURCEID, AE_ITEMKEYS[3]);

    public ArrayExpressAccessTool() {
        super(AE_NAME, AE_RESOURCEID, AE_STRUCTURE);
        try {
            this.getToolResource().setResourceURL(new URL(AE_URL));
            this.getToolResource().setResourceLogo(new URL(AE_LOGO));
            this.getToolResource().setResourceElementURL(AE_ELT_URL);
        } catch (MalformedURLException e) {
            logger.error(EMPTY_STRING, e);
        }
        this.getToolResource().setResourceDescription(AE_DESCRIPTION);
    }

    @Override
    public ResourceType getResourceType() {
        return ResourceType.SMALL;
    }

    @Override
    public void updateResourceInformation() {
        // TODO See if it can be implemented for this resource.
    }

    @Override
    public int updateResourceContent() {
        int nbElement = 0;
        ArrayExpressElement aeElement;
        Element element;
        //parse using builder to get DOM representation of the XML file
        Document dom = AbstractXmlResourceAccessTool.parseXML(AE_SERVICE);
        //get the root element
        org.w3c.dom.Element domRoot = dom.getDocumentElement();
        //get a nodelist of 'experiment' XML elements
        NodeList experimentList = domRoot.getElementsByTagName(AE_EXPERIMENT);
        if (experimentList != null && experimentList.getLength() > 0) {
            int listSize = experimentList.getLength();
            logger.info("Total number of elements on " + this.getToolResource().getResourceName() + ": " + listSize);
            // for each 'experiment' XML element
            for (int i = 0; i < listSize; i++) {
                aeElement = new ArrayExpressElement((org.w3c.dom.Element) experimentList.item(i), this);
                element = aeElement.getElement();
                if (element != null && resourceUpdateService.addElement(element)) {
                    nbElement++;
                }
            }
        }
        return nbElement;
    }

    @Override
    public String elementURLString(String elementLocalID) {
        return AE_ELT_URL + elementLocalID;
    }

    @Override
    public String mainContextDescriptor() {
        String AE_MAIN_ITEMKEY = "name";
        return AE_MAIN_ITEMKEY;
    }

    @Override
    //This function don't use ArrayExpressElement for optimization reasons.
    public HashSet<String> queryOnlineResource(String query) {
        HashSet<String> answerIDs = new HashSet<String>();

        // do not execute queryOnline for phrase with space
        String regexp = "\\S+\\s.+";
        if (!query.matches(regexp)) {
            String accnum = EMPTY_STRING;
            //parse using builder to get DOM representation of the XML file done with the query
            Document dom = AbstractXmlResourceAccessTool.parseXML(this.getXMLForQuery(query));
            //get the root element
            org.w3c.dom.Element domRoot = dom.getDocumentElement();
            //get a nodelist of 'experiment' XML elements
            NodeList experimentList = domRoot.getElementsByTagName(AE_EXPERIMENT);
            org.w3c.dom.Element experimentElt;

            if (experimentList != null && experimentList.getLength() > 0) {
                int listSize1 = experimentList.getLength();
                // for each 'experiment' XML element
                for (int i = 0; i < listSize1; i++) {
                    experimentElt = (org.w3c.dom.Element) experimentList.item(i);
                    accnum = experimentElt.getAttribute(ELT_ACCNUM);
                    answerIDs.add(accnum);
                }
            }
        }
        return answerIDs;
    }

    private String getXMLForQuery(String query) {
        return AE_SERVICE + "?keyword=" + query.replaceAll(BLANK_SPACE, "%20");
    }

    private class ArrayExpressElement {

        private final ArrayExpressAccessTool eltAETool;
        private final HashMap<String, String> eltInfo;

        ArrayExpressElement(org.w3c.dom.Element experimentElt, ArrayExpressAccessTool aeTool) {
            this.eltAETool = aeTool;
            this.eltInfo = new HashMap<String, String>(3);
            String nodeName = null;
            String description = EMPTY_STRING;
            String experimentType = EMPTY_STRING;
            String species = EMPTY_STRING;
            String name = EMPTY_STRING;
            String accession = null;
            for (int i = 0; i < experimentElt.getChildNodes().getLength(); i++) {
                Node node = experimentElt.getChildNodes().item(i);
                nodeName = node.getNodeName();

                if (ELT_ACCNUM.equals(nodeName)) { // Extracting accession
                    accession = node.getTextContent();
                } else if (AE_ITEMKEYS[0].equals(nodeName)) {// Extracting name
                    name = node.getTextContent();
                } else if (AE_ITEMKEYS[1].equals(nodeName)) { // Extracting Description
                    for (int j = 0; j < node.getChildNodes().getLength(); j++) {
                        if ("text".equals(node.getChildNodes().item(j).getNodeName())) {
                            description += BLANK_SPACE + node.getChildNodes().item(j).getTextContent();
                        }
                    }
                } else if (AE_ITEMKEYS[2].equals(nodeName)) {// Extracting species
                    species = resourceUpdateService.mapTermsToVirtualLocalConceptIDs(node.getTextContent(), AE_ONTOIDS[2], null);
                } else if (AE_EXPERIMENT_TYPE.equals(nodeName)) { // Extracting experiment type
                    experimentType = resourceUpdateService.mapTermsToVirtualLocalConceptIDs(node.getTextContent(), AE_ONTOIDS[3], null);
                }
            }

            this.eltInfo.put(ELT_ACCNUM, accession);
            this.eltInfo.put(ELT_NAME, name);
            this.eltInfo.put(ELT_DESCRIPTION, description.trim());
            this.eltInfo.put(ELT_SPECIES, species);
            this.eltInfo.put(ELT_EXPERIMENT_TYPE, experimentType);
        }

        Element getElement() {
            Element element = null;
            ArrayList<String> contextNames = this.eltAETool.getToolResource().getResourceStructure().getContextNames();
            Structure eltStructure = new Structure(contextNames);

            for (String contextName : contextNames) {
                eltStructure.putContext(contextName, this.eltInfo.get(contextName));
            }

            try {
                element = new Element(this.eltInfo.get(ELT_ACCNUM), eltStructure);
            } catch (BadElementStructureException e) {
                logger.error("** PROBLEM ** Cannot create Element for ArrayExpressElement with accnum: " + this.eltInfo.get(ELT_ACCNUM) + ". Null have been returned.", e);
            }
            return element;
        }
    }

}

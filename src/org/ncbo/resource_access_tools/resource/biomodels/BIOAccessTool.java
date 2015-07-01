/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.ncbo.resource_access_tools.resource.biomodels;

import org.ncbo.resource_access_tools.resource.AbstractXmlResourceAccessTool;
import org.ncbo.resource_access_tools.resource.nif.AbstractNifResourceAccessTool;
import org.ncbo.resource_access_tools.resource.nif.AbstractNifResourceAccessTool;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import obs.obr.populate.Element;
import obs.obr.populate.Structure;
import org.jsoup.Jsoup;
import org.ncbo.stanford.obr.enumeration.ResourceType;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import uk.ac.ebi.biomodels.ws.BioModelsWSClient;
import uk.ac.ebi.biomodels.ws.SimpleModel;

/**
 * AccessTool for BioModels resource
 * @author s.kharat
 */
public class BIOAccessTool extends AbstractNifResourceAccessTool {

    private static final String BIOM_URL = "http://www.ebi.ac.uk/biomodels-main/";
    private static final String BIOM_NAME = "BioModels";
    private static final String BIOM_RESOURCEID = "BIOM";
    private static final String BIOM_DESCRIPTION = "BioModels Database is a repository of peer-reviewed, published, computational models. These mathematical models are primarily from the field of systems biology, but more generally are those of biological interest. This resource allows biologists to store, search and retrieve published mathematical models. In addition, models in the database can be used to generate sub-models, can be simulated online, and can be converted between different representational formats. This resource also features programmatic access via Web Services.";
    private static final String BIOM_LOGO = "http://www.ebi.ac.uk/biomodels/icons/logo_small_border.png";
    private static final String BIOM_ELT_URL = "http://www.ebi.ac.uk/biomodels-main/";
    private static final String[] BIOM_ITEMKEYS = {"Name", "Publication_Id", "Notes", "Species", "Go_Terms", "Pathways"};
    private static final Double[] BIOM_WEIGHTS = {1.0, 0.5, 0.9, 0.7, 0.7, 0.7};
    private static final String[] BIOM_ONTOIDS = {Structure.FOR_CONCEPT_RECOGNITION, Structure.NOT_FOR_ANNOTATION, Structure.FOR_CONCEPT_RECOGNITION, Structure.FOR_CONCEPT_RECOGNITION, "1070", Structure.FOR_CONCEPT_RECOGNITION}; //"1070"
    private static Structure BIOM_STRUCTURE = new Structure(BIOM_ITEMKEYS, BIOM_RESOURCEID, BIOM_WEIGHTS, BIOM_ONTOIDS);
    private static String BIOM_MAIN_ITEMKEY = "Name";
    private Map<String, String> localOntologyIDMap;
    //constants
    private static final String Notes_tag = "notes";
    private static final String listofspecies_tag = "listOfSpecies";
    private static final String species_tag = "species";
    private static final String id_tag = "id";
    private static final String goprops = "urn:miriam:obo.go:";
    private static final String pathwayprops = "urn:miriam:kegg.pathway:";

    // constructors
    public BIOAccessTool() {
        super(BIOM_NAME, BIOM_RESOURCEID, BIOM_STRUCTURE);
        try {
            this.getToolResource().setResourceURL(new URL(BIOM_URL));
            this.getToolResource().setResourceDescription(BIOM_DESCRIPTION);
            this.getToolResource().setResourceLogo(new URL(BIOM_LOGO));
            this.getToolResource().setResourceElementURL(BIOM_ELT_URL);
        } catch (MalformedURLException e) {
            logger.error(EMPTY_STRING, e);
        }
        localOntologyIDMap = createLocalOntologyIDMap(BIOM_STRUCTURE);
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
        return BIOM_ELT_URL + elementLocalID;
    }

    @Override
    public String mainContextDescriptor() {
        return BIOM_MAIN_ITEMKEY;
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
                // populates OBR_BIOM_ET with each of these experiments.
                myExp = i.next();
                try {
                    if (!myExp.getElementStructure().hasNullValues()) {
                        if (this.resourceUpdateService.addElement(myExp)) {
                            nbElement++;
                        }
                    }
                } catch (Exception e) {
                    logger.error("** PROBLEM ** Problem with id " + myExp.getLocalElementId() + " when populating the OBR_BIOM_ET table.", e);
                }
            }
        } catch (Exception e) {
            logger.error("** PROBLEM ** Cannot update resource " + this.getToolResource().getResourceName(), e);
        }
        logger.info(nbElement + " elements added to the OBR_BIOM_ET table.");
        return nbElement;
    }

    /**
     * get all Elements.
     */
    public HashSet<Element> getAllElements() {
        logger.info("* Get All Elements for BioModels ... ");
        HashSet<Element> elementSet = new HashSet<Element>();
        int nbAdded = 0;
        String localElementId = EMPTY_STRING;

        try {
            //get all elements from _ET table
            HashSet<String> allElementsInET = this.resourceUpdateService.getAllLocalElementIDs();
            Map<String, Map<String, String>> allRowsData = new HashMap<String, Map<String, String>>();

            BioModelsWSClient client = new BioModelsWSClient();

            String[] modelsArray = client.getAllModelsId(); //getAllModelsId()
            logger.info("All Model size: " + modelsArray.length);

            List<String> modelsList = new ArrayList<String>();

            for (int i = 0; i < modelsArray.length; i++) {
                if (!(modelsArray[i].equals("MODEL1011090000") || modelsArray[i].equals("MODEL1106200000"))) {
                    //currently these models having some problems from resource site.
                    modelsList.add(modelsArray[i]);
                }
            }

            List<SimpleModel> resultSet = client.getSimpleModelsByIds(modelsList.toArray(new String[0]));
            //  List<SimpleModel> resultSet = client.getSimpleModelsByIds(modelsArray);
            Iterator iter = resultSet.iterator();

            while (iter.hasNext()) {
                Map<String, String> elementAttributes = new HashMap<String, String>();
                SimpleModel smodel = (SimpleModel) iter.next();

                localElementId = smodel.getId();
                elementAttributes.put(Structure.generateContextName(BIOM_RESOURCEID, BIOM_ITEMKEYS[0]), smodel.getName());
                elementAttributes.put(Structure.generateContextName(BIOM_RESOURCEID, BIOM_ITEMKEYS[1]), smodel.getPublicationId());

                String modeldata = client.getModelSBMLById(smodel.getId());

                Document dom = AbstractXmlResourceAccessTool.buildDom(modeldata);
                Node tableData = dom.getFirstChild();

                Node model = null;
                for (int i = 0; i < tableData.getChildNodes().getLength(); i++) {
                    if(tableData.getChildNodes().item(i).getNodeName().equalsIgnoreCase("model")){
                       model =  tableData.getChildNodes().item(i);
                    }
                }

                String goTerms = "";
                String pathway = "";

                for (int i = 0; i < model.getChildNodes().getLength(); i++) {

                    if (model.getChildNodes().item(i).getNodeName().equals(Notes_tag)) {
                        Node note = model.getChildNodes().item(i);
                        elementAttributes.put(Structure.generateContextName(BIOM_RESOURCEID, BIOM_ITEMKEYS[2]), Jsoup.parse(note.getTextContent()).text().replaceAll(whitespace_regx, BLANK_SPACE).trim());
                    } else if (model.getChildNodes().item(i).getNodeName().equals(listofspecies_tag)) {
                        StringBuffer species = new StringBuffer();
                        Node speciesList = model.getChildNodes().item(i);
                        for (int j = 0; j < speciesList.getChildNodes().getLength(); j++) {
                            if (speciesList.getChildNodes().item(j).getNodeName().equalsIgnoreCase(species_tag)) {
                                Node n = speciesList.getChildNodes().item(j);
                                Node name = n.getAttributes().getNamedItem(id_tag);
                                if (species.length() > 0) {
                                    species.append(COMMA_SEPARATOR);
                                }
                                species.append(name.getNodeValue());
                            }
                        }
                        elementAttributes.put(Structure.generateContextName(BIOM_RESOURCEID, BIOM_ITEMKEYS[3]), species.toString());
                    } else if (model.getChildNodes().item(i).getNodeName().equals("annotation")) {
                        Node annotationNode = model.getChildNodes().item(i);
                        for (int j = 0; j < annotationNode.getChildNodes().getLength(); j++) {
                            if (annotationNode.getChildNodes().item(j).getNodeName().equalsIgnoreCase("rdf:RDF")) {
                                Node rdfNode = model.getChildNodes().item(i).getChildNodes().item(j);
                                for (int k = 0; k < rdfNode.getChildNodes().getLength(); k++) {
                                    if (rdfNode.getChildNodes().item(k).getNodeName().equalsIgnoreCase("rdf:Description")) {
                                        for (int l = 0; l < rdfNode.getChildNodes().item(k).getChildNodes().getLength(); l++) {
                                            String nodeName = rdfNode.getChildNodes().item(k).getChildNodes().item(l).getNodeName();
                                            if (nodeName.equalsIgnoreCase("bqbiol:isVersionOf") || nodeName.equalsIgnoreCase("bqbiol:isPartOf")
                                                    || nodeName.equalsIgnoreCase("bqbiol:hasPart") || nodeName.equalsIgnoreCase("bqbiol:hasVersion") || nodeName.equalsIgnoreCase("bqbiol:is")) {
                                                Node gotag = rdfNode.getChildNodes().item(k).getChildNodes().item(l);

                                                for (int g = 0; g < gotag.getChildNodes().item(1).getChildNodes().getLength(); g++) {
                                                    if (gotag.getChildNodes().item(1).getChildNodes().item(g).getNodeName().equalsIgnoreCase("rdf:li")) {
                                                        String term = gotag.getChildNodes().item(1).getChildNodes().item(g).getAttributes().getNamedItem("rdf:resource").getNodeValue();
                                                        if (term.contains(goprops)) {
                                                            term = term.substring(term.lastIndexOf(":") + 1);
                                                            term = term.replaceAll("%3A", ":");
                                                            if (goTerms.length() > 0 && term.length() > 0) {
                                                                goTerms += GT_SEPARATOR_STRING;
                                                            }
                                                            goTerms += term;
                                                        } else if (term.contains(pathwayprops)) {
                                                            term = term.substring(term.lastIndexOf(":") + 1);
                                                            term = term.replaceAll("%3A", ":");
                                                            if (pathway.length() > 0 && term.length() > 0) {
                                                                pathway += COMMA_SEPARATOR;
                                                            }
                                                            pathway += term;
                                                        }
                                                    }
                                                }

                                            }
                                        }
                                    }
                                }
                            }
                        }
                        elementAttributes.put(Structure.generateContextName(BIOM_RESOURCEID, BIOM_ITEMKEYS[4]), goTerms);
                        elementAttributes.put(Structure.generateContextName(BIOM_RESOURCEID, BIOM_ITEMKEYS[5]), pathway);
                    }
                }

                //Check if elementId is present locally.
                if (allElementsInET.contains(localElementId)) {
                    continue;
                } else {
                    allRowsData.put(localElementId, elementAttributes);
                }
            }

            //parsing ends

            // Second phase: creation of elements
            for (String localElementID : allRowsData.keySet()) {
                Map<String, String> protAnnotAttribute2 = new HashMap<String, String>();
                protAnnotAttribute2 = allRowsData.get(localElementID);

                // PUT DATA INTO A STRUCTURE++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
                Structure elementStructure = new Structure(BIOM_STRUCTURE.getContextNames());
                for (String contextName : BIOM_STRUCTURE.getContextNames()) {
                    boolean attributeHasValue = false;

                    for (String att : protAnnotAttribute2.keySet()) {
                        if (contextName.equals(att)) {
                            // not an existing annotation
                            if (BIOM_STRUCTURE.getOntoID(contextName).equals(Structure.FOR_CONCEPT_RECOGNITION)
                                    || BIOM_STRUCTURE.getOntoID(contextName).equals(Structure.NOT_FOR_ANNOTATION)) {
                                elementStructure.putContext(contextName, protAnnotAttribute2.get(att));
                                attributeHasValue = true;

                            } else { // existing annotations
                                String localConceptID_leftPart = localOntologyIDMap.get(contextName); //ontoID
                                String localConceptID = EMPTY_STRING;
                                String localExistingAnnotations = protAnnotAttribute2.get(att).trim();
                                String localConceptIDs = EMPTY_STRING;
                                String[] splittedAnnotations = localExistingAnnotations.split(GT_SEPARATOR_STRING);
                                // translate conceptIDs used in the resource in OBR localConceptID
                                for (int i = 0; i < splittedAnnotations.length; i++) {
                                    try {
                                        String localConceptID_rightPart = splittedAnnotations[i].trim();
                                        if (!localConceptID_rightPart.isEmpty()) {
                                            localConceptID = localConceptID_leftPart + SLASH_STRING + localConceptID_rightPart;//localConceptID_leftPart+"/"+localConceptID_rightPart;
                                        }
                                    } catch (Exception e) {
                                        logger.error("** PROBLEM ** Problem with the management of the conceptID used in the resource to reported in the OBR", e);
                                    }
                                    if (localConceptID != EMPTY_STRING) {
                                        if (localConceptIDs != EMPTY_STRING) {
                                            localConceptIDs += GT_SEPARATOR_STRING + localConceptID;
                                        } else {
                                            localConceptIDs += localConceptID;
                                        }
                                    }
                                }
                                elementStructure.putContext(contextName, localConceptIDs);
                                attributeHasValue = true;
                            }// end of existing annotation
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

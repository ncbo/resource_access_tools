package org.ncbo.resource_access_tools.resource.pathwaycommons;

import org.biopax.paxtools.io.jena.JenaIOHandler;
import org.biopax.paxtools.model.BioPAXLevel;
import org.biopax.paxtools.model.Model;
import org.biopax.paxtools.model.level2.pathway;
import org.ncbo.resource_access_tools.common.utils.UnzipUtils;
import org.ncbo.resource_access_tools.enumeration.ResourceType;
import org.ncbo.resource_access_tools.populate.Element;
import org.ncbo.resource_access_tools.populate.Element.BadElementStructureException;
import org.ncbo.resource_access_tools.populate.Structure;
import org.ncbo.resource_access_tools.resource.ResourceAccessTool;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

/**
 * PathwayCommonsAccessTool is responsible for getting data elements for
 * PathwayCommons.
 * It process all pathways data available from different resources from PathwayCommons site.
 *
 * @author Kuladip Yadav & palanisamy
 * @version $$
 */
public class PathwayCommonsAccessTool extends ResourceAccessTool {

    // Home URL of the resource
    private static final String PC_URL = "http://www.pathwaycommons.org/pc/";

    // Name of the resource
    private static final String PC_NAME = "Pathway Commons";

    // Short name of the resource
    private static final String PC_RESOURCEID = "PC";

    // Text description of the resource
    private static final String PC_DESCRIPTION = "Pathway Commons is a convenient point of access to biological pathway information collected from public pathway databases, which you can browse or search.";

    // URL that points to the logo of the resource
    private static final String PC_LOGO = "http://www.pathwaycommons.org/pc/jsp/images/pathwaycommons/pc_logo.gif";

    //Optra: basic URL that points to an element when concatenated with an local element ID
    private static final String PC_ELT_URL = "http://www.pathwaycommons.org/pc/record2.do?id=";

    // The set of context names
    private static final String[] PC_ITEMKEYS = {"name", "organism", "comment"};

    // Weight associated to a context
    private static final Double[] PC_WEIGHTS = {1.0, 1.0, 0.9};

    // OntoID associated for reported annotations (NCBI organismal classification(1132) for organism)
    private static final String[] PC_ONTOIDS = {Structure.FOR_CONCEPT_RECOGNITION, "1132", Structure.FOR_CONCEPT_RECOGNITION};

    // Structure for PAthway Common Access tool
    private static final Structure PC_STRUCTURE = new Structure(PC_ITEMKEYS, PC_RESOURCEID, PC_WEIGHTS, PC_ONTOIDS);

    // A context name used to describe the associated element
    private static final String PC_MAIN_ITEMKEY = "name";

    // Path way data file down load location (URL)
    private static final String PC_URL_ZIP = "http://www.pathwaycommons.org/pc-snapshot/april-2011/biopax/by_source/";

    //Cancer cell map data source file name
    private static final String PC_CELL_MAP_ZIP = "cell-map.owl.zip";

    //Humancyc data source file name
    private static final String PC_HUMANCYC_ZIP = "humancyc.owl.zip";

    //NCI/Nature pathway interaction data source file name
    private static final String PC_NCI_NATURE_ZIP = "nci-nature.owl.zip";

    //Reactome data source file name
    //private static final String PC_REACTOME_ZIP="reactome.owl.zip";

    /**
     * Construct PathwayCommonsAccessTool using database connection property.
     * It set properties for tool Resource.
     */
    public PathwayCommonsAccessTool() {
        super(PC_NAME, PC_RESOURCEID, PC_STRUCTURE);
        try {
            this.getToolResource().setResourceURL(new URL(PC_URL));
            this.getToolResource().setResourceLogo(new URL(PC_LOGO));
            this.getToolResource().setResourceElementURL(PC_ELT_URL);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
        this.getToolResource().setResourceDescription(PC_DESCRIPTION);
    }

    @Override
    public ResourceType getResourceType() {
        return ResourceType.SMALL;
    }

    @Override
    public void updateResourceInformation() {
    }

    @Override
    public String mainContextDescriptor() {
        return PC_MAIN_ITEMKEY;
    }

    @Override
    public int updateResourceContent() {
        return updateAllElements();
    }

    /**
     * Update all the elements from owl extend files located in local path.
     *
     * @return number of elements updated
     */
    private int updateAllElements() {
        logger.info("Updating " + this.getToolResource().getResourceName() + " elements...");
        int nbElement = 0;
        try {
            //Extracts Cancer cell map file
            nbElement += extractZipFile(PC_URL_ZIP + PC_CELL_MAP_ZIP);

            //Extracts NCI/Nature pathway interaction file
            nbElement += extractZipFile(PC_URL_ZIP + PC_NCI_NATURE_ZIP);

            //Extracts Reactome map file
            //nbElement += extractZipFile( PC_URL_ZIP + PC_REACTOME_ZIP);

            //Extracts Humancyc file
            nbElement += extractZipFile(PC_URL_ZIP + PC_HUMANCYC_ZIP);
        } catch (FileNotFoundException e) {
            logger.error("** PROBLEM ** Cannot update resource " + this.getToolResource().getResourceName(), e);
        } catch (BadElementStructureException e) {
            logger.error("** PROBLEM ** Cannot update " + this.getToolResource().getResourceName() + " because of a Structure problem.", e);
        }
        return nbElement;
    }

    /**
     * Extracting zip files to local path using UnzipUtils.java class.
     * Removing file from local path.
     *
     * @param unzipUtils
     * @param fileName
     * @throws InvocationTargetException
     * @throws IllegalAccessException
     * @throws FileNotFoundException
     */

    private int extractZipFile(String fileName) throws FileNotFoundException, BadElementStructureException {
        // Download and extracts zip file
        int nbElement = 0;
        try {
            //Extracting ZIP file
            UnzipUtils.extractZipFile(fileName);
            //Parsing the file using Bio parser
            nbElement = parseOWL(UnzipUtils.getDataFile());
        } catch (FileNotFoundException ex) {
            throw ex;
        } catch (BadElementStructureException ex1) {
            throw ex1;
        } finally {
            UnzipUtils.deleteDataFile();
        }
        return nbElement;
    }

    /**
     * Parsing the extracted files
     *
     * @param file
     * @return
     * @throws FileNotFoundException
     * @throws BadElementStructureException
     */
    private int parseOWL(File file) throws FileNotFoundException, BadElementStructureException {
        int nbElement = 0;
        JenaIOHandler jenaIOHandler = new JenaIOHandler(null, BioPAXLevel.L2);
        Model model;
        try {
            model = jenaIOHandler.convertFromOWL(new FileInputStream(file));
            Set<pathway> pathways = model.getObjects(pathway.class);

            //Gets all table column names
            ArrayList<String> contextNames = this.getToolResource().getResourceStructure().getContextNames();
            Element element;
            Structure eltStructure = new Structure(contextNames);

            for (pathway mypathway : pathways) {

                //logger.info("NAME: "+ mypathway.getNAME());
                eltStructure.putContext(contextNames.get(0), mypathway.getNAME());

                //Organism Name
                String organismName = EMPTY_STRING;
                if (mypathway.getORGANISM() != null) {
                    organismName = mypathway.getORGANISM().getNAME();

                    if (organismName != null && organismName.trim().length() > 0) {
                        organismName = resourceUpdateService.mapTermsToVirtualLocalConceptIDs(organismName, PC_ONTOIDS[1], null);
                        // if mapping concepts are null or empty then log message for it.
                        if (organismName == null || organismName.trim().length() == 0) {
                            logger.error("Cannot map Organism  '" + mypathway.getORGANISM().getNAME() + "' to local concept id.");

                        }
                    }

                }
                //logger.info("ORGANISM: "+ organismName);
                eltStructure.putContext(contextNames.get(1), organismName);

                //Comment
                String comments = mypathway.getCOMMENT().toString().substring(1, mypathway.getCOMMENT().toString().length() - 1);
                //logger.info("COMMENT: "+comments);
                eltStructure.putContext(contextNames.get(2), comments);

                //path id or local element id
                String[] id = mypathway.getRDFId().split("\\-");
                //logger.info("ID: "+ id[id.length-1].trim());
                element = new Element(id[id.length - 1].trim(), eltStructure);

                //updating data into OBR_PC_ET table
                if (resourceUpdateService.addElement(element)) {
                    nbElement++;
                }
            }
        } catch (FileNotFoundException ex) {
            throw ex;
        } catch (BadElementStructureException ex1) {
            throw ex1;
        }
        return nbElement;
    }

    @Override
    public String elementURLString(String elementLocalID) {
        return PC_ELT_URL + elementLocalID;
    }

    public String itemKeyForAnnotationForBP() {
        return PC_MAIN_ITEMKEY;
    }

    @Override
    public HashSet<String> queryOnlineResource(String query) {
        return new HashSet<String>();
    }

}

package org.ncbo.resource_access_tools.service.resource.impl;

import org.apache.log4j.Logger;
import org.ncbo.resource_access_tools.exception.ResourceFileException;
import org.ncbo.resource_access_tools.populate.Element;
import org.ncbo.resource_access_tools.populate.Element.BadElementStructureException;
import org.ncbo.resource_access_tools.populate.Resource;
import org.ncbo.resource_access_tools.populate.Structure;
import org.ncbo.resource_access_tools.resource.ResourceAccessTool;
import org.ncbo.resource_access_tools.service.AbstractResourceService;
import org.ncbo.resource_access_tools.service.resource.ResourceUpdateService;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;

public class ResourceUpdateServiceImpl extends AbstractResourceService implements ResourceUpdateService {

    private static final Logger logger = Logger.getLogger(ResourceUpdateServiceImpl.class);

    /**
     * @param resourceAccessTool
     */
    public ResourceUpdateServiceImpl(ResourceAccessTool resourceAccessTool) {
        super(resourceAccessTool);
    }

    /**
     * Update the resource content according to a given tab delimited text file.
     * The first column of the file must be the elementLocalID.
     * This file must contain the same number of itemKey columns than the associated resource structure.
     * Returns the number of elements updated. Can be used for updateResourceContent.
     */
    public int updateResourceContentFromFile(File resourceFile) throws BadElementStructureException, ResourceFileException {
        int nbElement = 0;
        logger.info("Updating resource content with local file " + resourceFile.getName() + "...");
        try {
            FileReader fstream = new FileReader(resourceFile);
            BufferedReader in = new BufferedReader(fstream);
            String line = in.readLine();
            String[] elementCompleteInfo = line.split("\t");
            ArrayList<String> contextNames = resourceAccessTool.getToolResource().getResourceStructure().getContextNames();
            if (elementCompleteInfo.length != contextNames.size() + 1) {
                throw new ResourceFileException("Number of columns too short in file " + resourceFile.getName());
            }
            Element element;
            Structure eltStructure = new Structure(contextNames);
            while (line != null) {
                elementCompleteInfo = line.split("\t");
                int i = 0;
                for (String contextName : contextNames) {
                    eltStructure.putContext(contextName, elementCompleteInfo[i + 1]);
                    i++;
                }
                element = new Element(elementCompleteInfo[0], eltStructure);
                if (this.addElement(element)) {
                    nbElement++;
                }
                line = in.readLine();
            }
            in.close();
            fstream.close();
        } catch (IOException e) {
            logger.error("** PROBLEM ** Cannot update resource " + resourceAccessTool.getToolResource().getResourceName() + " with file " + resourceFile.getName(), e);
        }
        return nbElement;
    }

    /**
     * Adds the Resource tool entry into Resource table (OBR_RT)
     */
    public void addResource(Resource resource) {
        resourceTableDao.addEntryOrUpdate(resource);
    }

    public long numberOfEntry() {
        return elementTableDao.numberOfEntry();
    }

    public void reInitializeAllTables() {
        elementTableDao.reInitializeSQLTable();
        reInitializeAllTablesExcept_ET();
    }

    public void reInitializeAllTablesExcept_ET() {
        elementTableDao.resetDictionary();
        resourceTableDao.resetDictionary(resourceAccessTool.getToolResource().getResourceId());
    }

    /**
     * Returns a set of all the localElementIDs contained in the table.
     */
    public HashSet<String> getAllLocalElementIDs() {
        return elementTableDao.getAllLocalElementIDs();
    }

    /**
     * Returns a set of all the values contained in the given column of table.
     */
    public HashSet<String> getAllValuesByColumn(String columName) {
        return elementTableDao.getAllValuesByColumn(columName);
    }

    /**
     * Returns the value of a given context for a given element in the table.
     */
    public String getContextValueByContextName(String localElementID, String contextName) {
        return elementTableDao.getContextValueByContextName(localElementID, contextName);
    }

    public boolean addElement(Element element) {
        return elementTableDao.addEntry(element);
    }

    /**
     * This method split terms string with splitString
     * and get local concept id's using ontology access tool.
     *
     * @param terms
     * @param localOntologyID
     * @param splitString
     * @return String containing local concept id's separated by '>'
     */
    public String mapTermsToLocalConceptIDs(String terms, String localOntologyID, String splitString) {
        return "";
    }

    /**
     * This method gets local concept ids for Set of terms for given localOntologyID
     *
     * @param terms           Set of term strings.
     * @param localOntologyID
     * @return String containing local concept id's separated by '>'
     */
    private String mapTermsToLocalConceptIDs(HashSet<String> terms, String localOntologyID) {
        return "";
    }

    /* (non-Javadoc)
     * @see org.ncbo.stanford.obr.service.resource.ResourceUpdateService#mapTermsToVirtualLocalConceptIDs(java.lang.String, java.lang.String, java.lang.String)
     */
    public String mapTermsToVirtualLocalConceptIDs(String terms,
                                                   String virtualOntologyID, String splitString) {
        return EMPTY_STRING;
    }

    /*
     * (non-Javadoc)
     * @see org.ncbo.stanford.obr.service.resource.ResourceUpdateService#mapTermsToVirtualLocalConceptIDs(java.util.HashSet, java.lang.String, java.lang.String)
     */
    public String mapTermsToVirtualLocalConceptIDs(java.util.HashSet<String> terms, String virtualOntologyID) {
        return EMPTY_STRING;
    }

    /*
     * (non-Javadoc)
     * @see org.ncbo.stanford.obr.service.resource.ResourceUpdateService#getLocalConceptIdByPrefNameAndOntologyId(java.lang.String, java.lang.String)
     */
    public String getLocalConceptIdByPrefNameAndOntologyId(String virtualOntologyID, String termName) {
        return EMPTY_STRING;
    }

    /**
     * This method gets latest version of ontology for given virtual ontology id
     *
     * @param virtualOntologyID
     * @return String of latest version of ontology.
     */
    public String getLatestLocalOntologyID(String virtualOntologyID) {
        return "";
    }

    /**
     * Method update resource table with total number of element and update date.
     *
     * @param resource {@code Resource} to be updated.
     * @return boolean {@code true} if updated successfully.
     */
    public boolean updateResourceUpdateInfo(Resource resource) {
        // TODO Auto-generated method stub
        int totalElements = elementTableDao.getTotalNumberOfElement();
        return resourceTableDao.updateNumberOfElementAndDate(resource.getResourceId(), totalElements);

    }

    /**
     * Method update resource table after completion of resource workflow.
     * It includes updation of dictionary id and date for resource workflow completed.
     *
     * @param resource {@code Resource} to be updated.
     * @return boolean {@code true} if updated successfully.
     */
    public boolean updateResourceWorkflowInfo(Resource resource) {
        return resourceTableDao.updateDictionaryAndWorkflowDate(resource);

    }

    /**
     * This method get local element id for last element.
     *
     * @return localElementID
     */
    public String getLastElementLocalID() {
        return elementTableDao.getLastElementLocalID();
    }
}

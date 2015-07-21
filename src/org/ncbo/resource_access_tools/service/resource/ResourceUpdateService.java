package org.ncbo.resource_access_tools.service.resource;

import java.io.File;
import java.util.HashSet;

import org.ncbo.resource_access_tools.common.beans.DictionaryBean;
import org.ncbo.resource_access_tools.exception.ResourceFileException;
import org.ncbo.resource_access_tools.populate.Element;
import org.ncbo.resource_access_tools.populate.Resource;
import org.ncbo.resource_access_tools.populate.Element.BadElementStructureException;

/**
 * @author Kuladip Yadav
 *
 */
public interface ResourceUpdateService {
	
	/**
	 * 
	 * 
	 * @param toolResource
	 */
	public void addResource(Resource toolResource);
 
	/**
	 * @param resourceFile
	 * @return
	 * @throws ResourceFileException 
	 * @throws BadElementStructureException 
	 */
	public int updateResourceContentFromFile(File resourceFile) throws BadElementStructureException, ResourceFileException;

	/**
	 *
	 * Returns a set of all the localElementIDs contained in the table. 
	 *
	 * @return Collection of local_element_id strings.
	 */
	public HashSet<String> getAllLocalElementIDs();
	
	/**
	 * Returns a set of all the values contained in the given column of table. 
	 */
	public  HashSet<String> getAllValuesByColumn(String columName);

	/**
	 * Returns the value of a given context for a given element in the table.
	 */
	public String getContextValueByContextName(String localElementID, String contextName);

		
	/**
	 * This method split terms string with splitString
	 * and get local concept id's using ontology access tool. 
	 * 
	 * @param terms
	 * @param localOntologyID
	 * @param splitString
	 * @return String containing local concept id's separated by '>' 
	 */
	public String mapTermsToLocalConceptIDs(String terms, String localOntologyID, String splitString);
	
	/**
	 * This method split terms string with splitString
	 * and get local concept id's using ontology access tool. 
	 * 
	 * @param terms
	 * @param virtualOntologyID
	 * @param splitString
	 * 
	 */
	public String mapTermsToVirtualLocalConceptIDs(HashSet<String> terms, String virtualOntologyID);
	
	/**
	 * This method split terms string with splitString
	 * and get local concept id's using ontology access tool. 
	 * 
	 * @param terms
	 * @param virtualOntologyID
	 * @param splitString
	 * @return String containing local concept id's separated by '>' 
	 */
	public String mapTermsToVirtualLocalConceptIDs(String terms, String virtualOntologyID, String splitString);

	/**
	 * This method gets concept for given term name for given virtual ontology id
	 * 
	 * @param virtualOntologyID
	 * @param termName
	 * @return
	 */
	public String getLocalConceptIdByPrefNameAndOntologyId(String virtualOntologyID, String termName);
	
	/**
	 * Adds new entry for given @code Element in element table.
	 * 
	 * @param element
	 * @return boolean - true if successfully added otherwise false.
	 */
	public boolean addElement(Element element);
	
	/**
	 * Gets the number of entries present in element table.
	 * 
	 * @return long
	 */
	public long numberOfEntry();

	/**
	 * Remove all the entries from element table, annotation table, index table for
	 *  
	 */
	public void reInitializeAllTables();

	/**
	 * Remove all the entries from element table, annotation table, index table for
	 */
	public void reInitializeAllTablesExcept_ET();

	/**
	 * This method calculates number of aggregated annotations, mgrep annotations, reported annotations, isa annotations, mapping annotations
	 * for a resource.
	 * @param withCompleteDictionary 
	 * @param dictionary 
	 * 
	 */
	public void calculateObrStatistics(boolean withCompleteDictionary, DictionaryBean dictionary);
	
	/**
	 * This method gets latest version of ontology for given virtual ontology id
	 * 
	 * @param virtualOntologyID 
	 * @return String of latest version of ontology.
	 */
	public String getLatestLocalOntologyID(String virtualOntologyID);

	/**
	 * Method update resource table with total number of element and update date.
	 * 
	 * @param resource {@code Resource} to be updated. 
	 * @return boolean  {@code true} if updated successfully.
	 */
	public boolean updateResourceUpdateInfo(Resource resource);

	/**
	 * Method update resource table after completion of resource workflow.
	 * It includes updation of dictionary and date for resource workflow completed.
	 * 
	 * @param resource {@code Resource} to be updated. 
	 * @return boolean {@code true} if updated successfully.
	 */
	public boolean updateResourceWorkflowInfo(Resource resource); 
	
	/**
	 * This method get local element id for last element.
	 *    
	 * @return localElementID
	 */
	public String getLastElementLocalID();
}

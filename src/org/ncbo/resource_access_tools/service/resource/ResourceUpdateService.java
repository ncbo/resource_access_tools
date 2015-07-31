package org.ncbo.resource_access_tools.service.resource;

import java.io.File;
import java.util.HashSet;

import org.ncbo.resource_access_tools.exception.ResourceFileException;
import org.ncbo.resource_access_tools.populate.Element;
import org.ncbo.resource_access_tools.populate.Element.BadElementStructureException;

/**
 * @author Kuladip Yadav
 *
 */
public interface ResourceUpdateService {
	
	
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
	 * This method get local element id for last element.
	 *    
	 * @return localElementID
	 */
	public String getLastElementLocalID();
}

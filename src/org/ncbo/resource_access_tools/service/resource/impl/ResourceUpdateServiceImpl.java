package org.ncbo.resource_access_tools.service.resource.impl;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;

import org.apache.log4j.Logger;
import org.ncbo.resource_access_tools.exception.ResourceFileException;
import org.ncbo.resource_access_tools.populate.Element;
import org.ncbo.resource_access_tools.populate.Element.BadElementStructureException;
import org.ncbo.resource_access_tools.populate.Structure;
import org.ncbo.resource_access_tools.resource.ResourceAccessTool;
import org.ncbo.resource_access_tools.service.AbstractResourceService;
import org.ncbo.resource_access_tools.service.resource.ResourceUpdateService;

public class ResourceUpdateServiceImpl extends AbstractResourceService implements ResourceUpdateService{
	 
	// Logger for ResourceUpdateServiceImpl 
	protected static Logger logger = Logger.getLogger(ResourceUpdateServiceImpl.class);
 

	/**
	 * 
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
	public int updateResourceContentFromFile(File resourceFile) throws BadElementStructureException, ResourceFileException{
		int nbElement = 0;
		logger.info("Updating resource content with local file " + resourceFile.getName() + "...");
		try{
			FileReader fstream = new FileReader(resourceFile);
			BufferedReader in = new BufferedReader(fstream);
			String line = in.readLine();
			String[] elementCompleteInfo = line.split("\t");
			ArrayList<String> contextNames = resourceAccessTool.getToolResource().getResourceStructure().getContextNames(); 
			if (elementCompleteInfo.length != contextNames.size()+1){
				throw new ResourceFileException("Number of columns too short in file " + resourceFile.getName());
			} 
			Element element;
			Structure eltStructure = new Structure(contextNames);
			while (line != null){
				elementCompleteInfo = line.split("\t");
				int i=0;
				for(String contextName: contextNames){
					eltStructure.putContext(contextName, elementCompleteInfo[i+1]);
					i++;
				}
				element = new Element(elementCompleteInfo[0], eltStructure);
				if (this.addElement(element)){
					nbElement ++;
				}
				line = in.readLine();
			}
			in.close();
			fstream.close();
		}
		catch (IOException e){
			logger.error("** PROBLEM ** Cannot update resource " + resourceAccessTool.getToolResource().getResourceName() + " with file " + resourceFile.getName(), e);
		}
		return nbElement;
	}
	

	public long numberOfEntry() {		 
		return elementTableDao.numberOfEntry();
	} 

	public void reInitializeAllTables() {
		elementTableDao.reInitializeSQLTable();
	}	
	
	/**
	 * Returns a set of all the localElementIDs contained in the table. 
	 */
	public HashSet<String> getAllLocalElementIDs(){
		return elementTableDao.getAllLocalElementIDs();
	}
	
	/**
	 * Returns a set of all the values contained in the given column of table. 
	 */
	public HashSet<String> getAllValuesByColumn(String columName){
		return elementTableDao.getAllValuesByColumn(columName);
	}
	
	/**
	 * Returns the value of a given context for a given element in the table.
	 */
	public String getContextValueByContextName(String localElementID, String contextName){
		return elementTableDao.getContextValueByContextName(localElementID, contextName);
	}

	public boolean addElement(Element element){
		return elementTableDao.addEntry(element);
	}
	
	/**
	 * This method get local element id for last element.
	 *    
	 * @return localElementID
	 */
	public String getLastElementLocalID(){
		return elementTableDao.getLastElementLocalID();
	}
}

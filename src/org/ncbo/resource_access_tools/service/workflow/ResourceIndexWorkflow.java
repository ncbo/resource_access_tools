package org.ncbo.resource_access_tools.service.workflow;

import org.ncbo.resource_access_tools.dao.execution.ExecutionDao.ExecutionEntry;
import org.ncbo.resource_access_tools.resource.ResourceAccessTool;

/**
 * A service interface {@code ResourceIndexWorkflow} is main interface for workflow execution
 * it includes methods for populating slave obs tables and processing different resource tools with 
 * population of elements and indexing them using slave obs tables.
 *  
 * @author Kuladip Yadav
 */
public interface ResourceIndexWorkflow {

	/**
	 * This method populates slave obs tables from master obs tables which includes
	 * ontology table, concept table, term table, relation table and mapping table.
	 * 
	 * <p>This method compares slave and master ontology tables and populate newly added data in slave tables.
	 */
	public void populateObsSlaveTables() throws Exception;
	
	/**
	 * This method load obs table and stuff into memory.  
	 * It creates obs tables with MEMORY storage engine.
	 */
	public void loadObsSlaveTablesIntoMemory() throws Exception;
	
	/**
	 * This method includes complete resource index workflow. It process resources and 
	 * update elements for them and annotated them using obs tables.
	 * 
	 * <P>This methods process all the resources included in properties file.
	 * 
	 */
	public void startResourceIndexWorkflow();
	 
	/**
	 * This method process individual resource and update elements for it.
	 * Also it annotate that elements using obs tables and index them. 
	 * 
	 * @param resourceAccessTool a {@code ResourceAccessTool} to be processed. 
	 */
	public void resourceProcessing(ResourceAccessTool resourceAccessTool, ExecutionEntry executionEntry);
	
	/**
	 * Deletes the ontology duplicates from the OBS slave tables and all the resource index tables.
	 * which includes ontology table, concept table, term table, relation table and mapping table
	 * and annotations tables for all resources.
	 * 
	 * <p>This method ensures to keep only the latest version of ontologies.	  
	 */
	public void removeOntologyDuplicates();	
	
	/**
	 * This step execute replication mechanism between resource index 
	 * master/slave database.
	 * 
	 * @param replicateObsTables a {@code boolean} decide to copy obs tables or not . 
	 */
	public void executeSyncronizationScript(boolean replicateObsTables)throws Exception;
	
}

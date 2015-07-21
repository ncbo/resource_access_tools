package org.ncbo.resource_access_tools.service.workflow.impl;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import obs.common.utils.ExecutionTimer;

import org.apache.log4j.Logger;
import org.ncbo.resource_access_tools.common.beans.DictionaryBean;
import org.ncbo.resource_access_tools.common.beans.IsaContextBean;
import org.ncbo.resource_access_tools.common.beans.MappingContextBean;
import org.ncbo.resource_access_tools.common.beans.MgrepContextBean;
import org.ncbo.resource_access_tools.common.beans.OntologyBean;
import org.ncbo.resource_access_tools.common.beans.ReportedContextBean;
import org.ncbo.resource_access_tools.common.utils.Utilities;
import org.ncbo.resource_access_tools.dao.DaoFactory;
import org.ncbo.resource_access_tools.dao.execution.ExecutionDao.ExecutionEntry;
import org.ncbo.resource_access_tools.enumeration.ResourceType;
import org.ncbo.resource_access_tools.populate.ObrWeight;
import org.ncbo.resource_access_tools.resource.ResourceAccessTool;
import org.ncbo.resource_access_tools.service.obs.ObsDataPopulationService;
import org.ncbo.resource_access_tools.service.obs.impl.ObsDataPopulationServiceImpl;
import org.ncbo.resource_access_tools.service.workflow.ResourceIndexWorkflow;
import org.ncbo.resource_access_tools.util.FileResourceParameters;
import org.ncbo.resource_access_tools.util.LoggerUtils;
import org.ncbo.resource_access_tools.util.MessageUtils;
import org.ncbo.resource_access_tools.util.ProcessExecutor;
import org.ncbo.resource_access_tools.util.StringUtilities;
import org.ncbo.resource_access_tools.util.helper.StringHelper;



/**
 * A service  {@code ResourceIndexWorkflowImpl} is main service for workflow execution
 * it includes methods for populating slave obs tables and processing different resource tools with 
 * population of elements and indexing them using slave obs tables.
 * 
 * <p>Also, it includes functionality for removing duplicate ontologies.
 *  
 * @author Kuladip Yadav
 */
public class ResourceIndexWorkflowImpl implements ResourceIndexWorkflow, DaoFactory {

	// Logger for this class
	private static Logger logger;

	private static ObrWeight obrWeights = new ObrWeight(
			MgrepContextBean.PDA_WEIGHT, MgrepContextBean.SDA_WEIGHT,
			IsaContextBean.IEA_FACTOR, MappingContextBean.MEA_WEIGHT,
			ReportedContextBean.RDA_WEIGHT);
	
	private ObsDataPopulationService obsDataPopulationService = new ObsDataPopulationServiceImpl();
	 
	public ResourceIndexWorkflowImpl() {
		logger = LoggerUtils.createOBRLogger(ResourceIndexWorkflowImpl.class);
		// Disable sql logger
//		try {
//			AbstractObrDao.setSqlLogFile(new File("resource_index_workflow_sql.log"));
//		} catch (IOException e) {
//			logger.error("Problem in creating SQL log file.", e);
//		}
	}
	
	/**
	 * This method populates slave obs tables from master obs tables which includes
	 * ontology table, concept table, term table, relation table and mapping table.
	 * 
	 * <p>This method compares slave and master ontology tables and populate newly added data in slave tables.
	 */
	public void populateObsSlaveTables() throws Exception{
		 logger.info("Populating obs slave tables starts");	
		 boolean withLatestDictionary = Boolean.parseBoolean(MessageUtils.getMessage("obs.slave.dictionary.latest"));
		 
		 this.obsDataPopulationService.populateObsSlaveData(withLatestDictionary);			 
		 System.gc();
		 logger.info("Populating obs slave tables completed.");	
		
	}
	
	
	 /** (non-Javadoc)
	 * @see org.ncbo.stanford.obr.service.workflow.ResourceIndexWorkflow#loadObsSlaveTablesIntoMemeory()*/
	 
	public void loadObsSlaveTablesIntoMemory() throws Exception{
		 logger.info("Populating obs slave memory tables starts");		  
		 this.obsDataPopulationService.loadObsSlaveTablesIntoMemory();			 
		 System.gc();
		 logger.info("Populating obs slave memory completed.");	 		
	}

	/**
	 * This method includes complete resource index workflow. It process resources and 
	 * update elements for them and annotated them using obs tables.
	 * 
	 * <P>This methods process all the resources included in properties file.
	 * 
	 */
	//start
	public void startResourceIndexWorkflow() { 
		ExecutionTimer workflowTimer = new ExecutionTimer();
		workflowTimer.start();
		// gets all resource ids for processing, 
		String[] resourceIDs = StringUtilities.splitSecure(MessageUtils
				.getMessage("obr.resource.ids"), ",");		
		//Initialize the Execution timer 		
		ExecutionTimer timer = new ExecutionTimer();	
		logger.info("***********************************************\n");
		logger.info("The Resources index Workflow Started.\n");	
		for (String resourceID : resourceIDs) {
			ResourceAccessTool resourceAccessTool = null;
			ExecutionEntry executionEntry= new ExecutionEntry();
			executionEntry.setResourceId(resourceID);
			executionEntry.setExecutionBeginning(new Date());
			try {
				// Create resource tool object using reflection.
				resourceAccessTool = (ResourceAccessTool) Class.forName(
						MessageUtils.getMessage("resource."
								+ resourceID.toLowerCase())).newInstance();				 
				logger.info("Start processing Resource " + resourceAccessTool.getToolResource().getResourceName() + "("+ resourceAccessTool.getToolResource().getResourceId() + ")....\n");
				timer.start();
				resourceProcessing(resourceAccessTool, executionEntry);
				timer.end();
				logger.info("Resource " + resourceAccessTool.getToolResource().getResourceName() + "("+ resourceAccessTool.getToolResource().getResourceId() + ") processed in: " + timer.millisecondsToTimeString(timer.duration()) +"\n");
			} catch (Exception e) {
				logger.error(
						"Problem in creating resource tool for resource id : "
								+ resourceID, e);
			}finally{					
				resourceAccessTool= null;
				System.gc();
				executionEntry.setExecutionEnd(new Date());
				executionDao.addEntry(executionEntry);				
			}

		}
		workflowTimer.end();
		logger.info("Resources index Workflow completed in : " + workflowTimer.millisecondsToTimeString(workflowTimer.duration()));
		logger.info("***********************************************\n");
	}

	/**
	 * This method process individual resource and update elements for it.
	 * Also it annotate that elements using obs tables and index them. 
	 * 
	 * @param resourceAccessTool a {@code ResourceAccessTool} to be processed. 
	 * @param executionEntry 
	 */
	public void resourceProcessing(ResourceAccessTool resourceAccessTool, ExecutionEntry executionEntry) {
		ExecutionTimer timer = new ExecutionTimer();
		ExecutionTimer timer1 = new ExecutionTimer();
		
		boolean reInitializeAllTables =Boolean.parseBoolean(MessageUtils.getMessage("obr.reinitialize.all")) ;
		boolean reInitializeAllTablesExceptElement = Boolean.parseBoolean(MessageUtils
				.getMessage("obr.reinitialize.only.annotation")) ;
		boolean updateResource= Boolean.parseBoolean(MessageUtils.getMessage("obr.update.resource"));
		// value for withCompleteDictionary parameter.
		boolean withCompleteDictionary = Boolean.parseBoolean(MessageUtils
				.getMessage("obr.dictionary.complete"));
		
		// Creating logger for resourceAcessTool
		Logger toolLogger = ResourceAccessTool.getLogger();
		timer1.start();
		toolLogger.info("**** Resource "
				+ resourceAccessTool.getToolResource().getResourceId() + " processing");
		toolLogger.info("Workflow Parameters[withCompleteDictionary= " 	+ withCompleteDictionary 
									+ ", updateResource= " + updateResource 
									+ ", reInitializeAllTables= " + reInitializeAllTables
									+ ", reInitializeAllTablesExceptElement= " + reInitializeAllTablesExceptElement
									+ " ]");
		// Adds resource entry into Resource Table(OBR_RT)
		resourceAccessTool.addResourceTableEntry();

		// Re-initialized tables
		if (reInitializeAllTables) {
			resourceAccessTool.reInitializeAllTables();
		} else if (reInitializeAllTablesExceptElement) {
			resourceAccessTool.reInitializeAllTablesExcept_ET();
		}

		logger.info("\n");
		if(resourceAccessTool.numberOfElement()==0){
			executionEntry.setFirstExecution(true);
		}
		// Update resource for new elements 
		if (updateResource) {
			timer.start();
			toolLogger.info("*** Resource "
					+ resourceAccessTool.getToolResource().getResourceName() + " update processing");
			int nbElement = resourceAccessTool.updateResourceContent();
			resourceAccessTool.updateResourceUpdateInfo();
			
			timer.end();
			toolLogger.info("### Resource "
					+ resourceAccessTool.getToolResource().getResourceName()
					+ " updated with " + nbElement + " elements in : " + timer.millisecondsToTimeString(timer.duration()) +"\n");				 
		}

		// Get the latest dictionary from OBS_DVT
 		DictionaryBean dictionary = dictionaryDao.getLastDictionaryBean();
	 	
  	    // Adding into execution entry.
		executionEntry.setDictionaryId(dictionary.getDictionaryId());		
 		
		// Adding into execution entry.
		executionEntry.setWithCompleteDictionary(withCompleteDictionary);
		
		//resourceAccessTool.calculateObrStatistics(withCompleteDictionary, dictionary);//Added by jay		
		executionEntry.setNbElement(resourceAccessTool.getAnnotationService().getNumberOfElementsForAnnotation(dictionary.getDictionaryId()));
		
		 
		// Execute the workflow according to resource type.		
		executeWorkflow(resourceAccessTool, dictionary, withCompleteDictionary, toolLogger);//jay comment
		 
	    // Update resource table entry for latest dictionary and date for resource workflow completed
		resourceAccessTool.updateResourceWorkflowInfo();//jay comment
	    
		timer1.end();
		toolLogger.info("#### Resource " + resourceAccessTool.getToolResource().getResourceName()
				+ " processed in: "
				+ timer1.millisecondsToTimeString(timer1.duration()));		
	}
	//end
	
	/**
	 * This method execute resource. 
	 * 
	 * <p>After creating direct annotations perform expanded annotation on newly created direct
	 * annotation and index it. 
	 * 
	 * @param resourceAccessTool {@code ResourceAccessTool}
	 * @param dictionary {@code DictionaryBean) containing latest dictionary
	 * @param toolLogger {@code Logger} object for given resourceAccessTool
	 */
	private long executeWorkflow(ResourceAccessTool resourceAccessTool, DictionaryBean dictionary, boolean withCompleteDictionary, Logger toolLogger){
		
		ExecutionTimer timer = new ExecutionTimer();
		
		boolean disableStatistics = Boolean.parseBoolean(MessageUtils.getMessage("obr.statistics.populate"));
		
		// Total number of entries found in element table for annotation.	
		int nbEntry  = resourceAccessTool.getAnnotationService()
							.getNumberOfElementsForAnnotation(dictionary.getDictionaryId());
		
		
		if(nbEntry == 0){
			logger.info("\tNo element present for annotation for resource : " + resourceAccessTool.getToolResource().getResourceId());
			return 0;
		}

	    boolean disableIndexes = Boolean.parseBoolean(MessageUtils.getMessage("obr.table.index.disabled"));
	    long nbAggregatedAnnotation = 0;
	    
		if(disableIndexes){
			toolLogger.info("*** Disabling indexes on annotation tables starts...");
 			timer.reset();
 			timer.start();
			resourceAccessTool.getAnnotationService().disableIndexes();
			timer.end();
			toolLogger.info("### Disabling indexes on annotation tables completed in "
					+ timer.millisecondsToTimeString(timer.duration()) +".\n");			 
		} 
		
		try{
						 
			// Processing direct annotations
			long nbDirectAnnotation = resourceAccessTool.getAnnotationService()
					.resourceAnnotation(withCompleteDictionary, dictionary, 
							Utilities.arrayToHashSet(FileResourceParameters.STOP_WORDS)); 
			
			
			toolLogger.info(nbEntry + " elements annotated (with "
					+ nbDirectAnnotation
					+ " new direct annotations) from resource "
					+ resourceAccessTool.getToolResource().getResourceId() + ".\n");

			// Flag for mapping expansion.  
			boolean isaClosureExpansion = Boolean.parseBoolean(MessageUtils
					.getMessage("obr.expansion.relational"));
			
			// Flag for mapping expansion.
			boolean mappingExpansion = Boolean.parseBoolean(MessageUtils
					.getMessage("obr.expansion.mapping"));
			
			// Flag for distance expansion.
			boolean distanceExpansion = Boolean.parseBoolean(MessageUtils
					.getMessage("obr.expansion.distance"));

			// Creating semantic expansion annotation.
			long nbExpandedAnnotation = resourceAccessTool.getSemanticExpansionService()
					.semanticExpansion(isaClosureExpansion, mappingExpansion,
							distanceExpansion);
			toolLogger.info(nbEntry + " elements annotated (with "
					+ nbExpandedAnnotation
					+ " new expanded annotations) from resource "
					+ resourceAccessTool.getToolResource().getResourceId() + ".\n");
		}finally{
			if(disableIndexes){
				toolLogger.info("*** Enabling indexes on annotation tables starts...");
	 			timer.reset();
	 			timer.start();
				resourceAccessTool.getAnnotationService().enableIndexes(ResourceType.BIG==resourceAccessTool.getResourceType());
				timer.end();
				toolLogger.info("### Enabling indexes on annotation tables completed in "
						+ timer.millisecondsToTimeString(timer.duration()) +".\n");
			}  
		}
		// Aggregation step to annotations.	 
		nbAggregatedAnnotation = resourceAccessTool.getAggregationService().aggregation(
				obrWeights);
		 
		toolLogger.info(nbEntry + " elements aggregated (with "
				+ nbAggregatedAnnotation
				+ " new aggregated annotations) from resource "
				+ resourceAccessTool.getToolResource().getResourceId() + ".\n");
		
		// Sorting aggregation
		resourceAccessTool.getAggregationService().sortAggregation(resourceAccessTool.getResourceType());
		
		// Update obr_statistics and concept_frequency table.
		if(nbAggregatedAnnotation > 0) {
			resourceAccessTool.calulateConceptFrequncy();
			if(!disableStatistics)
				resourceAccessTool.calculateObrStatistics(withCompleteDictionary, dictionary);
			
		}  
		 
		return nbAggregatedAnnotation;   
	} 
 
	/**
	 * Deletes the ontology duplicates from the OBS slave tables and all the resource index tables.
	 * which includes ontology table, concept table, term table, relation table and mapping table
	 * and annotations tables for all resources.
	 *  
	 * <p>This method ensures to keep only the latest version of ontologies.	  
	 */
	public void removeOntologyDuplicates() {
		
		boolean removeOntologiesFromList = Boolean.parseBoolean(MessageUtils.getMessage("obs.slave.ontology.remove.from.list"));
		
		if(removeOntologiesFromList){
			String[] ontologyIDs = StringUtilities.splitSecure(MessageUtils
					.getMessage("obs.slave.ontology.remove.list"), ",");
			 removeSpecificOntologies(ontologyIDs);
			 return;
		}
		
		ExecutionTimer timer = new ExecutionTimer();
		timer.start();
		
		logger.info("*** Remove ontology duplicates started....");
			
		Set<String> ontologiesToRemove = new HashSet<String>();		 
		String virtualOntologyID1;
		String localOntologyID1;
		String virtualOntologyID2;
		String localOntologyID2;
		// Get all ontology beans from ontology tables.
		List<OntologyBean> allOntologyBeans = ontologyDao.getAllOntologyBeans();
		
		// Check for duplicate ontologies i.e two or more versions of same ontology
		for (OntologyBean ontologyBean1 : allOntologyBeans) {
			localOntologyID1= ontologyBean1.getLocalOntologyId();
			virtualOntologyID1 =ontologyBean1.getVirtualOntologyId();
			// traverses all the ontologies of the the OBS DB
			for (OntologyBean ontologyBean2 : allOntologyBeans) {
				localOntologyID2= ontologyBean2.getLocalOntologyId();
				virtualOntologyID2= ontologyBean2.getVirtualOntologyId();				
				// searches for duplicates
				if(virtualOntologyID1.equals(virtualOntologyID2) && !localOntologyID1.equals(localOntologyID2) ){
					// removes the ontology with the smallest localOntologyID (that situation will should happen only for BioPortal ontologies)
					if(Integer.parseInt(localOntologyID1)>Integer.parseInt(localOntologyID2)){
						ontologiesToRemove.add(localOntologyID2);
					}
					else{
						ontologiesToRemove.add(localOntologyID1);
					}
				}
			}
		} 
		 
		if(ontologiesToRemove.size() == 0){
			logger.info("\tNo ontology found to remove");					 
		}else {			
			removeOntologies(new ArrayList<String>(ontologiesToRemove)); 
		} 		
		timer.end();		
		logger.info("### Remove dupicate ontologies completed in: "
			+ timer.millisecondsToTimeString(timer.duration()) + ".\n");
	}
	
	private void removeSpecificOntologies(String[] localOntologyIds) {
		ExecutionTimer timer = new ExecutionTimer();
		timer.start();		
		logger.info("*** Remove ontologies started....");
		
		if(localOntologyIds == null || localOntologyIds.length == 0){
			logger.info("\tNo ontology found to remove");					 
		}else {	
			logger.info("\t Ontologies to remove : "+ Arrays.asList(localOntologyIds));
			removeOntologies(Arrays.asList(localOntologyIds)); 
		} 	
		
		timer.end();		
		logger.info("### Remove ontologies completed in: "
			+ timer.millisecondsToTimeString(timer.duration()) + ".\n");
		
	}
	
	private void removeOntologies(List<String> ontologiesToRemove){
		// remove from obr tables.
		 removeOntologiesFromOBRTables(ontologiesToRemove);
		 
		 // Iterating each duplicate ontology version and remove from obr and obs tables.
		
		logger.info("\t**Removing ontology version from obs slave tables started");	
		for (String localOntologyID : ontologiesToRemove) {
			logger.info("\t\tRemoving ontology version :" + localOntologyID);			 
			// remove ontology from obs slave database.
			obsDataPopulationService.removeOntology(localOntologyID);
		} 
		logger.info("\t##Removing ontology version from obs slave tables completed.");	
	}
	
	/**
	 * Remove all the annotations done by given by localOntology for all the resources.
	 * 
	 * @param localOntologyID ontology version
	 */
	private void removeOntologiesFromOBRTables(List<String> localOntologyIDs){
		// gets all resource ids for processing, 
		String[] resourceIDs = StringUtilities.splitSecure(MessageUtils
				.getMessage("obr.resource.ids"), ",");
		//Initialize the Execution timer 
		ExecutionTimer timer = new ExecutionTimer();	
		ExecutionTimer resourceTimer = new ExecutionTimer();	
		timer.start();
		logger.info("\t**The Remove ontologies from OBR tables Started.");	
		for (String resourceID : resourceIDs) {
			ResourceAccessTool resourceAccessTool = null;
			try {
				// Create resource tool object using reflection.
				resourceAccessTool = (ResourceAccessTool) Class.forName(
						MessageUtils.getMessage("resource."
								+ resourceID.toLowerCase())).newInstance();	
				resourceTimer.reset();
				resourceTimer.start();
				resourceAccessTool.removeOntologies(localOntologyIDs); 
				resourceTimer.end();
				logger.info("\t\tRemove ontologies from resource " + resourceID +" completed in : " + resourceTimer.millisecondsToTimeString(resourceTimer.duration()));
				
			} catch (Exception e) {
				logger.error(
						"Problem in creating resource tool for resource id : "
								+ resourceID, e);
			}finally{				
				resourceAccessTool= null;
				System.gc();
			}

		}		
		
		// Tracker item #2230
		// Deleting entries from statistics table.
		// statisticsDao.deleteEntriesFromOntologies(localOntologyIDs);
		
		timer.end();
		logger.info("\t## The Remove ontology from OBR tables processed in: " + timer.millisecondsToTimeString(timer.duration()));
		 
	}

	/**
	 * This step execute replication mechanism between resource index 
	 * master/slave database. 
	 * 
	 *  @param replicateObsTables a {@code boolean} decide to copy obs tables or not .
	 */
	public void executeSyncronizationScript(boolean replicateObsTables) throws Exception{
		ExecutionTimer timer = new ExecutionTimer();
		ProcessExecutor processExecutor = new ProcessExecutor(logger);
		
		String[] resourceIDs = StringUtilities.splitSecure(MessageUtils
				.getMessage("obr.resource.ids"), StringHelper.COMMA_STRING);	
		String syncScriptPath = MessageUtils.getMessage("obr.database.sync.script.path");
		 
		if(!new File(syncScriptPath).exists()){
			logger.error("Synchronization script not found at location " + syncScriptPath);
			throw new Exception("Synchronization script not found at location " + syncScriptPath);
		}
		
		timer.start();
		logger.info("Started executing syncronization script....");
		processExecutor.executeShellScript(syncScriptPath, true, true, replicateObsTables, resourceIDs);
		timer.end();
		logger.info("Syncronization script execution completed in : "
			+ timer.millisecondsToTimeString(timer.duration()) + ".\n");
	}
 
}

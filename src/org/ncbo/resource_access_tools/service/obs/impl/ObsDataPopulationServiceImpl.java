package org.ncbo.resource_access_tools.service.obs.impl;

import java.io.File;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import obs.common.utils.ExecutionTimer;

import org.apache.log4j.Logger;
import org.ncbo.resource_access_tools.common.beans.DictionaryBean;
import org.ncbo.resource_access_tools.common.utils.Utilities;
import org.ncbo.resource_access_tools.dao.DaoFactory;
import org.ncbo.resource_access_tools.dao.obs.master.ObsMasterDao;
import org.ncbo.resource_access_tools.dao.ontology.OntologyDao.OntologyEntry;
import org.ncbo.resource_access_tools.exception.NoOntologyFoundException;
import org.ncbo.resource_access_tools.service.obs.ObsDataPopulationService;
import org.ncbo.resource_access_tools.util.LoggerUtils;

/**
 * This service class {@code ObsDataPopulationServiceImpl} is provides implementation for populating obs slave data from master table which is used for
 * populating resource index.
 *  
 * @author Kuladip Yadav
 */
public class ObsDataPopulationServiceImpl implements ObsDataPopulationService, DaoFactory{
	 
	/** Default logger for {@code ObsDataPopulationServiceImpl} class. */
	private static Logger logger;
	
	/** The obsMasterDao used for querying OBS master database. */
	private ObsMasterDao obsMasterDao;
	
	/**
	 * 
	 */
	public ObsDataPopulationServiceImpl() {
		logger = LoggerUtils.createOBRLogger(ObsDataPopulationServiceImpl.class);
	} 
	 
	/**
	 * Populates all the OBS master tables in the right sequence in order to traverse ontologies only once and reuse
	 * what has been already processed.
	 *
	 * @param withLatestDictionary if true then will populate OBS data with latest dictionary without creating new dictionary
	 * 							   if false, a new dictionary row in dictionary table is created.
	 */	
	public void populateObsSlaveData(boolean withLatestDictionary) throws NoOntologyFoundException {
		// initialize obs master dao
		if(obsMasterDao== null){
			obsMasterDao = ObsMasterDao.getInstance();
		}
		 
		//Initialize the Execution timer 
     	ExecutionTimer timer = new ExecutionTimer();
     	timer.start();
     	logger.info("Population of slave data from master obs database started.");
		// populates or reuse dictionary 
		if(dictionaryDao.numberOfEntry()==0 || !withLatestDictionary){
			dictionaryDao.addEntry(DictionaryBean.DICO_NAME+Utilities.getRandomString(4));
		}
		// Getting latest dictionary.
		int dictionaryID = dictionaryDao.getLastDictionaryBean().getDictionaryId();
		// Get ontologies available currently in slave table. 
		List<String> currentSlaveOntologies = ontologyDao.getAllLocalOntologyIDs();
		List<String>  localOntologyIDs = populateOntologySlaveData(dictionaryID, currentSlaveOntologies);
	
		if(localOntologyIDs != null && localOntologyIDs.size()>0){
			populateConceptsSlaveData(localOntologyIDs);
		    populateTermsSlaveData(localOntologyIDs);		 
			populateRelationSlaveData(localOntologyIDs);
			populateMappingSlaveData(localOntologyIDs);
			populateSemanticTypeData(localOntologyIDs);
			populateLSemanticTypeData();
		}else{
			logger.info("No new ontology found in master table.");					
		}
		logger.info("Population of slave data from master obs database completed.");
		timer.end();		
		logger.info("Population of slave data processed in : " + timer.millisecondsToTimeString(timer.duration()));
		// Release the master database connection.
		obsMasterDao.closeConnection();
		
		if(localOntologyIDs == null || localOntologyIDs.size()==0){
			throw new NoOntologyFoundException();
		}
	}
 
	/**
	 * Populates new ontology versions present in OBS master database which are not present in 
	 * slave ontology table with particular dictionary. 
	 *  
	 * @param dictionaryID ID assign to new ontology versions from master table.
	 * @param currentSlavLocalOntologyIDs {@code List} of local ontology ids currently present in slave database.
	 * @return a list of newly populated local ontology ids from master table.
	 */
	public List<String> populateOntologySlaveData(int dictionaryID, List<String> currentSlavLocalOntologyIDs) {
		List<OntologyEntry>  ontologyEnties= obsMasterDao.getMasterOntologyEntries(dictionaryID, currentSlavLocalOntologyIDs);
		List<String> localOntologyIDs = new ArrayList<String>();
		int numberOfOntologiesAdded= ontologyDao.addEntries(ontologyEnties);
		// Populate obr ontology table
		obrOntologyDao.addEntries(ontologyEnties);	
		// Adding populated local ontology ids in list.
		for (OntologyEntry ontologyEntry : ontologyEnties) {
			localOntologyIDs.add(ontologyEntry.getLocalOntologyId());
		}		
		logger.info("Number of ontology entries added in slave ontology table : " + numberOfOntologiesAdded);
		
		return localOntologyIDs;
	} 
	
	/**
	 * Populates new concepts presents in OBS master database which are not present in 
	 * slave concept table for given ontology versions {@code localOntologyIDs}.
	 * 
	 * @param localOntologyIDs a {@code List} of local ontology ids.
	 * @return Number of concept entries added in slave concept table.
	 */
	public long populateConceptsSlaveData(List<String> localOntologyIDs) {
		long numberOfConceptsAdded= 0;
		File conceptEntryFile = null;		
		try{
			// Writes concept entries to file from master concept table. 
			conceptEntryFile = obsMasterDao.writeMasterConceptEntries(localOntologyIDs); 
			// load file entries into slave concept table. 
			numberOfConceptsAdded = conceptDao.populateSlaveConceptTableFromFile(conceptEntryFile);
			logger.info("Number of concept entries added in slave concept table : " + numberOfConceptsAdded);
		}finally {
			 // Delete generated file.
			 if(conceptEntryFile!= null && conceptEntryFile.exists()){
				// conceptEntryFile.delete();
			 }
		}		
		return numberOfConceptsAdded;
	} 
	
	/**
	 * Populates new term presents in OBS master database which are not present in 
	 * slave term table for given ontology versions {@code localOntologyIDs}.
	 * 
	 * @param localOntologyIDs a {@code List} of local ontology ids.
	 * @return Number of term entries added in slave term table.
	 */
	public long populateTermsSlaveData(List<String> localOntologyIDs) {		
		long numberOfTermsAdded= 0;
		long numberOfStopwordsTermsRemoved= 0;
		File termsEntryFile = null;		
		try{
			// Writes term entries to file from master term table.
			termsEntryFile = obsMasterDao.writeMasterTermEntries(localOntologyIDs); 
			// Load file entries into slave term table. 
			numberOfTermsAdded = termDao.populateSlaveTermTableFromFile(termsEntryFile);
			logger.info("Number of term entries added in slave term table : " + numberOfTermsAdded);
			numberOfStopwordsTermsRemoved = termDao.deleteEntriesForStopWords();
			logger.info("Number of stopword term entries removed from slave term table : " + numberOfStopwordsTermsRemoved);
			logger.info("Total Number of term entries added in slave term table : " + (numberOfTermsAdded - numberOfStopwordsTermsRemoved));
		}finally {
			 // Delete generated file.
			 if(termsEntryFile!= null && termsEntryFile.exists()){
				 termsEntryFile.delete();
			 }
		}		
		return numberOfTermsAdded- numberOfStopwordsTermsRemoved;
	} 
	
	/**
	 * Populates <b>is a parent</b> relation table entries presents in OBS master database which are not present in 
	 * slave relation table for given ontology versions {@code localOntologyIDs}.
	 * 
	 * @param localOntologyIDs a {@code List} of local ontology ids.
	 * @return Number of relation entries added in slave relation table.
	 */
	public long populateRelationSlaveData(List<String> localOntologyIDs){	
		long numberOfRelationsAdded= 0;
		File relationEntryFile = null;		
		try{
			// Writes 'is a parent' relation entries to file from master relation table.
			relationEntryFile = obsMasterDao.writeMasterRelationEntries(localOntologyIDs);
			// Load file entries into slave term table. 
			numberOfRelationsAdded = relationDao.populateSlaveRelationTableFromFile(relationEntryFile);
			logger.info("Total Number of relations entries added in slave relation table : " + numberOfRelationsAdded);
		}finally {
			 if(relationEntryFile!= null && relationEntryFile.exists()){
				 relationEntryFile.delete();
			 }
		}
		return numberOfRelationsAdded;
	} 
	 
	/**
	 * Populates new concept mapping entries presents in OBS master database which are not present in 
	 * slave mapping table for given ontology versions {@code localOntologyIDs}.
	 * 
	 * @param localOntologyIDs a {@code List} of local ontology ids.
	 * @return Number of mapping entries added in slave map table.
	 */
	public long populateMappingSlaveData(List<String> localOntologyIDs){	
		long numberOfMappingsAdded = 0 ;
		File mappingEntryFile = null;
		try{
			logger.info("Re-initialize slave Mapping table.");
			// Remove all data from mapping table.			
			mapDao.reInitializeSQLTable();
			// Writes mapping entries to file from master map table.
			mappingEntryFile = obsMasterDao.writeMasterMappingEntries();
			// Load file entries into slave mapping table. 
			numberOfMappingsAdded = mapDao.populateSlaveMappingTableFromFile(mappingEntryFile);			
			logger.info("Total Number of mapping entries added in slave map table : " + numberOfMappingsAdded);
		    
			long mappingTypeEntries= mapDao.populateMappingTypeTable();
			logger.info("Total Number of mapping type entries added in slave mapping_type table : " + mappingTypeEntries);
		}finally {
			 if(mappingEntryFile!= null && mappingEntryFile.exists()){
				 mappingEntryFile.delete();
			 }
		}
		return numberOfMappingsAdded;
	}
	
	/**
	 * Populates <b>is a parent</b> relation table entries presents in OBS master database which are not present in 
	 * slave relation table for given ontology versions {@code localOntologyIDs}.
	 * 
	 * @param localOntologyIDs a {@code List} of local ontology ids.
	 * @return Number of relation entries added in slave relation table.
	 */
	public long populateSemanticTypeData(List<String> localOntologyIDs){	
		long numberOfSemanticTypeAdded= 0;
		File semanticTypeEntryFile = null;		
		try{
			// Writes 'is a parent' relation entries to file from master relation table.
			semanticTypeEntryFile = obsMasterDao.writeMasterSemanticTypeEntries(localOntologyIDs);
			// Load file entries into slave term table. 
			numberOfSemanticTypeAdded = semanticTypeDao.populateSlaveSemanticTypeTableFromFile(semanticTypeEntryFile) ;
			logger.info("Total Number of Semantic Type entries added in slave relation table : " + numberOfSemanticTypeAdded);
		}finally {
			 if(semanticTypeEntryFile!= null && semanticTypeEntryFile.exists()){
				 semanticTypeEntryFile.delete();
			 }
		}  
		return numberOfSemanticTypeAdded;
	} 
	
	/**
	 * Populates <b>is a parent</b> relation table entries presents in OBS master database which are not present in 
	 * slave LSemanticType table for given ontology versions {@code localOntologyIDs}.
	 *  
	 * @return Number of LSemanticType entries added in slave relation table.
	 */
	public long populateLSemanticTypeData(){
		long numberOfSemanticTypeAdded= 0;
		File semanticTypeEntryFile = null;	
		try{
			logger.info("Re-initialize slave lSemanticType table.");
			// Remove all data from mapping table.			
			lSemanticTypeDao.reInitializeSQLTable();
			// Writes 'is a parent' relation entries to file from master LSemanticType table.
			semanticTypeEntryFile = obsMasterDao.writeMasterLSemanticTypeEntries();
			// Load file entries into slave term table. 
			numberOfSemanticTypeAdded = lSemanticTypeDao.populateSlaveLSemanticTypeTableFromFile(semanticTypeEntryFile) ;
			logger.info("Total Number of L Semantic Type entries added in slave relation table : " + numberOfSemanticTypeAdded);
		}finally {
			 if(semanticTypeEntryFile!= null && semanticTypeEntryFile.exists()){
				 semanticTypeEntryFile.delete();
			 }
		} 
		return numberOfSemanticTypeAdded;
		
	}

	/**
	 * This method gets list of ontology versions present in master ontology id
	 * 
	 * @return {@code List} of local ontology id
	 */
	public List<String> getMasterSlaveLocalOntologyIDs() {		
		 return obsMasterDao.getAllLocalOntologyIDs();
	}

	/**
	 * This method removes given ontology version from all the obs slave tables
	 * i.e. removes entries from table obs_ontology, obs_concept, obs_term, obs_relation, obs_map
	 * 
	 * @param localOntologyID ontology version to remove. 
	 */
	public void removeOntology(String localOntologyID) {
		boolean status = false;
		 // remove ontology from relation table
		 status =relationDao.deleteEntriesFromOntology(localOntologyID);
		 if(!status){
			 logger.error("Problem in removing ontology version " + localOntologyID + " from relation table.");
		 }
		 
		 // remove ontology from relation table
		 status =semanticTypeDao.deleteEntriesFromOntology(localOntologyID);
		 if(!status){
			 logger.error("Problem in removing ontology version " + localOntologyID + " from relation table.");
		 }
		 
		 // remove ontology from map table
		 status = mapDao.deleteEntriesFromOntology(localOntologyID);
		 if(!status){
			 logger.error("Problem in removing ontology version " + localOntologyID + " from mapping table.");
		 }
		 
		 // remove ontology from term table
		 status =termDao.deleteEntriesFromOntology(localOntologyID);
		 if(!status){
			 logger.error("Problem in removing ontology version " + localOntologyID + " from term table.");
		 }
		 
		 // remove ontology from concept table
		 status = conceptDao.deleteEntriesFromOntology(localOntologyID);
		 if(!status){
			 logger.error("Problem in removing ontology version " + localOntologyID + " from concept table.");
		 }
		 
		 // remove ontology from ontology table
		 status = ontologyDao.deleteEntriesFromOntology(localOntologyID);
		 if(!status){
			 logger.error("Problem in removing ontology version " + localOntologyID + " from ontology table.");
		 }
	}
	
	/*
	 * (non-Javadoc)
	 * @see org.ncbo.stanford.obr.service.obs.ObsDataPopulationService#loadObsSlaveTablesIntoMemeory()
	 */
	public void loadObsSlaveTablesIntoMemory() throws SQLException {
		ontologyDao.callLoadObsSlaveTablesIntoMemoryProcedure();
	}
}

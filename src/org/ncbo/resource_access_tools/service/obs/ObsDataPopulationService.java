package org.ncbo.resource_access_tools.service.obs;

import java.sql.SQLException;
import java.util.List;

/**
 * This service interface {@code ObsDataPopulationService} is used for populating obs slave data from master table which is used for
 * populating resource index
 *
 * @author Kuladip Yadav
 *
 */
public interface ObsDataPopulationService {

	/**
	 * Populates all the OBS master tables in the right sequence in order to traverse ontologies only once and reuse
	 * what has been already processed.
	 *
	 * @param withLatestDictionary if true then will populate OBS data with latest dictionary without creating new dictionary
	 * 							   if false, a new dictionary row in dictionary table is created.
	 */
	public void populateObsSlaveData(boolean withLatestDictionary) throws NoOntologyFoundException;

	/**
	 * This method load obs table and stuff into memory.
	 * It creates obs tables with MEMORY storage engin.
	 */
	public void loadObsSlaveTablesIntoMemory() throws SQLException;

	/**
	 * Populates new ontology versions present in OBS master database which are not present in
	 * slave ontology table with particular dictionary.
	 *
	 * @param dictionaryID ID assign to new ontology versions from master table.
	 * @param currentSlavLocalOntologyIDs {@code List} of local ontology ids currently present in slave database.
	 * @return a list of newly populated local ontology ids from master table.
	 */
	public List<String> populateOntologySlaveData(int dictionaryID, List<String> currentSlavLocalOntologyIDs);

	/**
	 * Populates new concepts presents in OBS master database which are not present in
	 * slave concept table for given ontology versions {@code localOntologyIDs}.
	 *
	 * @param localOntologyIDs a {@code List} of local ontology ids.
	 * @return Number of concept entries added in slave concept table.
	 */
	public long populateConceptsSlaveData(List<String> localOntologyIDs);

	/**
	 * Populates new term presents in OBS master database which are not present in
	 * slave term table for given ontology versions {@code localOntologyIDs}.
	 *
	 * @param localOntologyIDs a {@code List} of local ontology ids.
	 * @return Number of term entries added in slave term table.
	 */
	public long populateTermsSlaveData(List<String> localOntologyIDs);

	/**
	 * Populates <b>is a parent</b> relation table entries presents in OBS master database which are not present in
	 * slave relation table for given ontology versions {@code localOntologyIDs}.
	 *
	 * @param localOntologyIDs a {@code List} of local ontology ids.
	 * @return Number of relation entries added in slave relation table.
	 */
	public long populateRelationSlaveData(List<String> localOntologyIDs);

	/**
	 * Populates new concept mapping entries presents in OBS master database which are not present in
	 * slave mapping table for given ontology versions {@code localOntologyIDs}.
	 *
	 * @param localOntologyIDs a {@code List} of local ontology ids.
	 * @return Number of mapping entries added in slave map table.
	 */
	public long populateMappingSlaveData(List<String> localOntologyIDs);

	/**
	 * This method gets list of ontology versions present in master ontology id
	 *
	 * @return {@code List} of local ontology id
	 */
	public List<String> getMasterSlaveLocalOntologyIDs();

	/**
	 * This method removes given ontology version from all the obs slave tables
	 * i.e. removes entries from table obs_ontology, obs_concept, obs_term, obs_relation, obs_map
	 *
	 * @param localOntologyID ontology version to remove.
	 */
	public void removeOntology(String localOntologyID);
}

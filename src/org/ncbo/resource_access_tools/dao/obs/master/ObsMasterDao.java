package org.ncbo.resource_access_tools.dao.obs.master;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.ncbo.resource_access_tools.dao.DaoFactory;
import org.ncbo.resource_access_tools.dao.ontology.OntologyDao.OntologyEntry;
import org.ncbo.resource_access_tools.util.FileResourceParameters;
import org.ncbo.resource_access_tools.util.MessageUtils;

import com.mysql.jdbc.CommunicationsException;
import com.mysql.jdbc.exceptions.MySQLNonTransientConnectionException;
/**
 * This class is served as data access object for obs master tables.
 * It is used to query the different tables in obs like ontology table, concept table, term table, 
 * relation table and mapping table. 
 * 
 * @author Kuladip Yadav
 */
public class ObsMasterDao implements DaoFactory{

	/** Default logger for {@code ObsMasterDao} class. */
	protected static Logger logger = Logger.getLogger(ObsMasterDao.class);
	/** This is obs master database connection. */
	private static Connection masterTableConnection;
	/** sql statement for master*/
	private static Statement tableStatement; 
	
	/** JDBC connection parameter for master obs tables. */
	private static final String MASTER_OBS_CONNECTION_STRING = MessageUtils.getMessage("obs.master.jdbc.url");
	private static final String DATABASE_JDBC_DRIVER = MessageUtils.getMessage("obr.jdbc.driver");
	private static final String MASTER_OBS_USER = MessageUtils.getMessage("obs.master.jdbc.username");
	private static final String MASTER_OBS_PASSWORD = MessageUtils.getMessage("obs.master.jdbc.password");
	
	private static final String MASTER_OBS_SCEHMA_NAME = MessageUtils.getMessage("obs.master.schema.name");
	private static final String MASTER_OBS_HOST_NAME = MessageUtils.getMessage("obs.master.schema.host.name");
  
	/** Constant for ontology complete status for master obs_ontology table.  */
	private static final int ONTOLOGY_COMPLETE_STATUS = Integer.parseInt(MessageUtils.getMessage("obs.master.ontology.status.complete"));
	
	/** Constant for concept table entries file. */
	private static final String CONCEPT_ENTRIES_FILENAME = "OBS_MASTER_CONCEPT_TABLE";
	/** Constant for term table entries file. */
	private static final String TERM_ENTRIES_FILENAME = "OBS_MASTER_TERM_TABLE";
	/** Constant for relation table entries file. */
	private static final String RELATION_ENTRIES_FILENAME = "OBS_MASTER_RELATION_TABLE";
	/** Constant for mapping table entries file. */
	private static final String MAPPING_ENTRIES_FILENAME = "OBS_MASTER_MAPPING_TABLE";	
	/** Constant for semantic type table entries file. */
	private static final String SEMANTIC_ENTRIES_FILENAME = "OBS_MASTER_SEMATIC_TYPE_TABLE";
	
	/**
	 * Default constructor for {@code ObsMasterDao}.
	 * Making as private for using singleton pattern. 
	 */
	private ObsMasterDao() {
		this.createConnection();
	}

	/** Instance holder for ObsMasterDao. */ 
	private static class ObsMasterDaoHolder {
		private final static ObsMasterDao OBS_MASTER_DAO_INSTANCE = new ObsMasterDao();
	}

	/**
	 * Returns a ObsMasterDao object by creating one if a singleton not already exists.
	 */
	public static ObsMasterDao getInstance(){
		return ObsMasterDaoHolder.OBS_MASTER_DAO_INSTANCE;
	}
	
	/**
	 * Creates masterTableConnection for master obs database if not created previously
	 */
	private void createConnection(){
		if(masterTableConnection == null){
			try{
				Class.forName(DATABASE_JDBC_DRIVER).newInstance();						
				masterTableConnection = DriverManager.getConnection(MASTER_OBS_CONNECTION_STRING, MASTER_OBS_USER, MASTER_OBS_PASSWORD);
				masterTableConnection.setReadOnly(true);
			}
			catch(Exception e){
				logger.error("** PROBLEM ** Cannot create connection to database " + MASTER_OBS_CONNECTION_STRING, e);
			}
		}
	}
	
	/**
	 * This method release the connection 
	 * 
	 */
	public void closeConnection(){
		 try{
			 if(tableStatement!= null) {
				 tableStatement.close();	
				 tableStatement= null;
			 } 
		 }catch (Exception e) {
			logger.error("Problem in closing statement " , e); 
		} finally{
			try{
				if(masterTableConnection!= null){
					masterTableConnection.close();
					masterTableConnection=null;
				}
			}catch (SQLException e) {
				logger.error("Problem in closing connection " , e); 
			}
			
			 
		}
		 
	}
	
	/**
	 * Reopens the DB connection if closed and reopens all the prepared statement for all instances of sub-classes.
	 */
	private static void reOpenConnectionIfClosed(){
		try{
			if (masterTableConnection.isClosed()){
				masterTableConnection = DriverManager.getConnection(MASTER_OBS_CONNECTION_STRING, MASTER_OBS_USER, MASTER_OBS_PASSWORD);
				masterTableConnection.setReadOnly(true);
				logger.info("\t[SQL Connection just reopenned.]");
			}
		}
		catch(SQLException e){
			logger.error("** PROBLEM ** Cannot create connection to database " + MASTER_OBS_CONNECTION_STRING, e);
		}
	}

	
	/**
	 * Executes a given SQL query as String using a generic statement. As it returns a ResultSet,
	 * this statement needs to be explicitly closed after the processing of the ResultSet with function
	 *
	 * @param query String representing sql query to be executed.
	 * @return {@code ResultSet} contains result after executing {@code query}.
	 */
	protected ResultSet executeSQLQuery(String query) throws SQLException {
		ResultSet rSet;
		try{
			tableStatement = masterTableConnection.createStatement(ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_READ_ONLY);
			rSet = tableStatement.executeQuery(query);
		}
		catch (CommunicationsException e) {
			// Reopen connection 
			reOpenConnectionIfClosed();
			tableStatement = masterTableConnection.createStatement(ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_READ_ONLY);
			rSet = tableStatement.executeQuery(query);
		}			 
		return rSet;
	}	

	/**
	 * Method get new versions of ontologies from obs master table which are not present 
	 * in slave tables and populate them in {@code OntologyEntry} beans.
	 * 
	 * @param dictionaryID {@code int} id used populate {@code OntologyEntry}.
	 * @param slaveLocalOntologyIDs {@code List} of local ontology ids not included in result.
	 * @return {@code List} of {@code OntologyEntry} each representing single entry form master ontology table.
	 */
	public List<OntologyEntry> getMasterOntologyEntries(int dictionaryID, List<String> slaveLocalOntologyIDs){
		List<OntologyEntry> ontologyEntries = new ArrayList<OntologyEntry>();
		
		StringBuffer selectQuery = new StringBuffer();
		selectQuery.append("SELECT OT.id, OT.local_ontology_id, OT.name, OT.version, OT.description, OT.status, OT.virtual_ontology_id, OT.format FROM ");
		selectQuery.append(ontologyDao.getTableSQLName());
		selectQuery.append(" OT WHERE OT.status= ") ;
		selectQuery.append(ONTOLOGY_COMPLETE_STATUS);
		selectQuery.append("; ");
		
		try {			 			
			ResultSet rSet = this.executeSQLQuery(selectQuery.toString());
			OntologyEntry ontologyEntry;
			while(rSet.next()){
				// Skipped ontology version already present in slave ontology list. 
				if(slaveLocalOntologyIDs.contains(rSet.getString(2))){
					continue;
				}
				// Creating ontologyEntry.
				ontologyEntry = new OntologyEntry(rSet.getInt(1), rSet.getString(2),  rSet.getString(3),  rSet.getString(4),
						rSet.getString(5), rSet.getInt(6),  rSet.getString(7),  rSet.getString(8), dictionaryID);
				
				ontologyEntries.add(ontologyEntry);
			}			
			rSet.close();
		}
		catch (MySQLNonTransientConnectionException e) {			 
			return this.getMasterOntologyEntries(dictionaryID,slaveLocalOntologyIDs);
		}
		catch (SQLException e) {
			logger.error("** PROBLEM ** Cannot get ontology entries from master ontology table.", e);
		}
		
		return ontologyEntries;
	} 
	 
	/**
	 * This method get concept table entries from master concept tables for given ontology versions
	 * and write the result into text file.
	 * 
	 * @param localOntologyIDs list of local ontology ids.
	 * @return {@code File} containing concept entries.
	 */
	public File writeMasterConceptEntries(List<String> localOntologyIDs){		 
		StringBuffer selectQuery = new StringBuffer();
		selectQuery.append("SELECT CT.id, CT.local_concept_id, CT.ontology_id, CT.is_toplevel, CT.full_id FROM ");
		selectQuery.append(conceptDao.getTableSQLName());
		selectQuery.append(" CT, ");
		selectQuery.append(ontologyDao.getTableSQLName());
		selectQuery.append(" OT WHERE CT.ontology_id = OT.id AND OT.local_ontology_id IN(");		
		for (String localOntologyID : localOntologyIDs) {
			selectQuery.append(localOntologyID);
			selectQuery.append(", ");
		}		
		selectQuery.delete(selectQuery.length()-2, selectQuery.length());
		selectQuery.append(");");
		
		try {		
			// Write query result to file.
			return writeQueryResultFile(selectQuery.toString(), CONCEPT_ENTRIES_FILENAME);		
		}  
		catch ( Exception e) {
			logger.error("** PROBLEM ** Cannot get entries from master concept table.", e);
		}		
		return null;
	}
	
	/**
	 * This method get term table entries from master term tables for given ontology versions
	 * and write the result into text file.
	 * 
	 * @param localOntologyIDs list of local ontology ids.
	 * @return {@code File} containing term entries.
	 */
	public File writeMasterTermEntries(List<String> localOntologyIDs){		 
		StringBuffer selectQuery = new StringBuffer();
		selectQuery.append("SELECT TT.id, TT.name, TT.concept_id, TT.is_preferred FROM ");
		selectQuery.append(termDao.getTableSQLName());
		selectQuery.append(" TT, ");
		selectQuery.append(conceptDao.getTableSQLName());
		selectQuery.append(" CT, ");
		selectQuery.append(ontologyDao.getTableSQLName());
		selectQuery.append(" OT WHERE TT.concept_id = CT.id AND CT.ontology_id = OT.id AND OT.local_ontology_id IN(");		
		for (String localOntologyID : localOntologyIDs) {
			selectQuery.append(localOntologyID);
			selectQuery.append(", ");
		}		
		selectQuery.delete(selectQuery.length()-2, selectQuery.length());
		selectQuery.append(");");	
		
		try {			 						
			return writeQueryResultFile(selectQuery.toString(), TERM_ENTRIES_FILENAME);		
		}  
		catch ( Exception e) {
			logger.error("** PROBLEM ** Cannot get entries from master term table.", e);
		}		
		return null;
	}
	
	/**
	 * This method get relation table entries from master relation tables for given ontology versions
	 * and write the result into text file.
	 * 
	 * @param localOntologyIDs list of local ontology ids.
	 * @return {@code File} containing relation entries.
	 */
	public File writeMasterRelationEntries(List<String> localOntologyIDs){		 
		StringBuffer selectQuery = new StringBuffer();
		selectQuery.append("SELECT ISAPT.id, ISAPT.concept_id, ISAPT.parent_concept_id, ISAPT.level FROM ");
		selectQuery.append(relationDao.getTableSQLName());
		selectQuery.append(" ISAPT, ");
		selectQuery.append(conceptDao.getTableSQLName());
		selectQuery.append(" CT, ");
		selectQuery.append(ontologyDao.getTableSQLName());
		selectQuery.append(" OT WHERE ISAPT.concept_id = CT.id AND CT.ontology_id = OT.id AND OT.local_ontology_id IN (");		
		for (String localOntologyID : localOntologyIDs) {
			selectQuery.append(localOntologyID);
			selectQuery.append(", ");
		}		
		selectQuery.delete(selectQuery.length()-2, selectQuery.length());
		selectQuery.append("); ");	
		
		try {		
			return writeQueryResultFile(selectQuery.toString(), RELATION_ENTRIES_FILENAME);			
		}  
		catch ( Exception e) {
			logger.error("** PROBLEM ** Cannot get entries from master ralation table.", e);
		}		
		return null;
	}
	
	/**
	 * This method get mapping table entries from master map tables for given ontology versions
	 * and write the result into text file.
	 * 
	 * @param localOntologyIDs list of local ontology ids.
	 * @return {@code File} containing mapping entries.
	 */
	public File writeMasterMappingEntries(){		 
		StringBuffer selectQuery = new StringBuffer();
		selectQuery.append("SELECT MAPT.id,  MAPT.concept_id,  MAPT.mapped_concept_id, MAPT.mapping_type FROM ");
		selectQuery.append(mapDao.getTableSQLName());
		selectQuery.append(" MAPT; ");
		
		try {	
			return writeQueryResultFile(selectQuery.toString(), MAPPING_ENTRIES_FILENAME);
		}  
		catch ( Exception e) {
			logger.error("** PROBLEM ** Cannot get entries from master ralation table.", e);
		}
		
		return null;
	}
	
	/**
	 * This method get SemanticType table entries from master relation tables for given ontology versions
	 * and write the result into text file.
	 * 
	 * @param localOntologyIDs list of local ontology ids.
	 * @return {@code File} containing relation entries.
	 */
	public File writeMasterSemanticTypeEntries(List<String> localOntologyIDs){		 
		StringBuffer selectQuery = new StringBuffer();
		selectQuery.append("SELECT ST.id, ST.concept_id, ST.semantic_type_id FROM ");
		selectQuery.append(semanticTypeDao.getTableSQLName());
		selectQuery.append(" ST, ");
		selectQuery.append(conceptDao.getTableSQLName());
		selectQuery.append(" CT, ");
		selectQuery.append(ontologyDao.getTableSQLName());
		selectQuery.append(" OT WHERE ST.concept_id = CT.id AND CT.ontology_id = OT.id AND OT.local_ontology_id IN (");		
		for (String localOntologyID : localOntologyIDs) {
			selectQuery.append(localOntologyID);
			selectQuery.append(", ");
		}		
		selectQuery.delete(selectQuery.length()-2, selectQuery.length());
		selectQuery.append("); ");	
		
		try {		
			return writeQueryResultFile(selectQuery.toString(), SEMANTIC_ENTRIES_FILENAME);			
		}  
		catch ( Exception e) {
			logger.error("** PROBLEM ** Cannot get entries from master ralation table.", e);
		}		
		return null;
	}
	
	/**
	 * This method get LSemanticType table entries from master relation tables for given ontology versions
	 * and write the result into text file.
	 * 
	 * @param localOntologyIDs list of local ontology ids.
	 * @return {@code File} containing relation entries.
	 */
	public File writeMasterLSemanticTypeEntries(){		 
		StringBuffer selectQuery = new StringBuffer();
		selectQuery.append("SELECT ST.id, ST.semantic_type, ST.description FROM ");
		selectQuery.append(lSemanticTypeDao.getTableSQLName());
		selectQuery.append(" ST;");			
		try {		
			return writeQueryResultFile(selectQuery.toString(), SEMANTIC_ENTRIES_FILENAME);			
		}  
		catch ( Exception e) {
			logger.error("** PROBLEM ** Cannot get entries from master ralation table.", e);
		}		
		return null;
	}
	
	
	/**
	 * Method executes mysql command on master database by firing given {@code sqlQuery} 
	 * and write result in to file.
	 * 
	 * <p>Method uses obs master database credentials as follows:
	 * <pre>
	 * 	Host name as {@link #MASTER_OBS_HOST_NAME MASTER_OBS_HOST_NAME}
	 * 	Database Schema as {@link #MASTER_OBS_SCEHMA_NAME MASTER_OBS_SCEHMA_NAME}
	 * 	Username as {@link #MASTER_OBS_USER MASTER_OBS_USER}
	 * 	Password as {@link #MASTER_OBS_PASSWORD MASTER_OBS_PASSWORD}
	 * </pre>
	
	 * 
	 * @param sqlQuery a query to be executed on obs master table.
	 * @param fileName Name of the text file used to write result. 
	 * @return {@code File} containing result entries for given {@code sqlQuery}.
	 * @throws Exception 
	 */
	public File writeQueryResultFile(String sqlQuery, String fileName) throws Exception{		
		File outputFile = new File(FileResourceParameters.dictionaryFolder() + fileName + ".txt");
		FileWriter fw = new FileWriter(outputFile);
		// Creating mysql command
		StringBuffer command = new StringBuffer();
		command.append("mysql -h ");
		command.append(MASTER_OBS_HOST_NAME);
		command.append(" -u ");
		command.append(MASTER_OBS_USER);
		command.append(" -p");
		command.append(MASTER_OBS_PASSWORD);
		command.append(" ");
		command.append(MASTER_OBS_SCEHMA_NAME);
		command.append(" -e \"");
		command.append(sqlQuery);
		command.append("\" > '");
		command.append(outputFile.getAbsolutePath());
		command.append("' ");		
		String[] mysqlCommand = {"/bin/sh", "-c", command.toString()};				
		Process p = Runtime.getRuntime().exec(mysqlCommand);
		int exitValue = p.waitFor();
		
		
		if (exitValue != 0){
			logger.info("Problem during the mysql command execution...");
			BufferedReader stdErr = new BufferedReader(new InputStreamReader(p.getErrorStream()));
			// read the output from the command
			String s;
			while ((s = stdErr.readLine()) != null) {
				logger.error(s);
			}
		}
		return outputFile;		
	}

	/**
	 * This method gets all the ontology versions available in ontology table
	 *  
	 * @return {@code List} of local ontology ids.
	 */
	public List<String> getAllLocalOntologyIDs() {
		List<String> ontologyIDs = new ArrayList<String>();
		StringBuffer selectQuery = new StringBuffer();
		selectQuery.append("SELECT  OT.local_ontology_id FROM ");
		selectQuery.append(ontologyDao.getTableSQLName());
		selectQuery.append(" OT; ");
		
		try {			 			
			ResultSet rSet = this.executeSQLQuery(selectQuery.toString());
			 
			while(rSet.next()){
				ontologyIDs.add(rSet.getString(1));
			}
		} 
		catch (MySQLNonTransientConnectionException e) {			 
			return this.getAllLocalOntologyIDs();
		}
		catch (SQLException e) {
			logger.error("** PROBLEM ** Cannot get ontology ids from master ontology table.", e);
		}
		
		return ontologyIDs;
	}
	
}

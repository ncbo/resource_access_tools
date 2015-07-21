package org.ncbo.resource_access_tools.dao.ontology;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import org.ncbo.resource_access_tools.common.beans.OntologyBean;
import org.ncbo.resource_access_tools.dao.AbstractObrDao;
import org.ncbo.resource_access_tools.dao.concept.ConceptDao;
import org.ncbo.resource_access_tools.dao.ontology.OntologyDao.OntologyEntry;
import org.ncbo.resource_access_tools.dao.term.TermDao;
import org.ncbo.resource_access_tools.util.MessageUtils;

import com.mysql.jdbc.exceptions.MySQLIntegrityConstraintViolationException;
import com.mysql.jdbc.exceptions.MySQLNonTransientConnectionException;

/**
 * This class is a representation for the OBS(slave) DB obs_ontology table. The table contains 
 * the following columns:
 * <ul>
   <li>id INT(11) NOT NULL PRIMARY KEY
   <li>local_ontology_id VARCHAR(246) NOT NULL UNIQUE
   <li>name VARCHAR(246) NOT NULL
   <li>version VARCHAR(246) NOT NULL
   <li>description VARCHAR(246) NOT NULL
   <li>status INT(11) NOT NULL
   <li>virtual_ontology_id VARCHAR(246) NOT NULL
   <li>format VARCHAR(32) DEFAULT NULL
 * </ul>
 * 
 */
public class ObrOntologyDao extends AbstractObrDao{
	
	private static final String TABLE_SUFFIX = MessageUtils.getMessage("obs.ontology.table.suffix");

	private PreparedStatement addEntryStatement;	
	private static PreparedStatement getEntryStatement;
	private static PreparedStatement getLatestLocalOntologyIDStatement;
	private static PreparedStatement hasNewVersionOfOntologyStatement;
	private static PreparedStatement getLocalConceptIdByPrefNameAndOntologyIdStatement;
	private static PreparedStatement getAllLocalOntologyIDsStatement;
	private static PreparedStatement deleteEntriesFromOntologyStatement;
	private static PreparedStatement getAllOntologyBeansStatement;
	
	
	private ObrOntologyDao() {		
		super(EMPTY_STRING, TABLE_SUFFIX);
	} 
	
	private static class OntologyDaoHolder {
		private final static ObrOntologyDao ONTOLOGY_DAO_INSTANCE = new ObrOntologyDao();
	}
	
	/**
	 * Returns a OntologyTable object by creating one if a singleton not already exists.
	 */
	public static ObrOntologyDao getInstance(){
		return OntologyDaoHolder.ONTOLOGY_DAO_INSTANCE;
	}
	
	public static String name(){		
		return OBS_PREFIX + TABLE_SUFFIX;
	}
	
	@Override
	protected String creationQuery(){
		return "CREATE TABLE " + this.getTableSQLName() +" (" +
		"id INT(11) NOT NULL AUTO_INCREMENT PRIMARY KEY, " +
		"local_ontology_id VARCHAR(246) NOT NULL UNIQUE, " +
		"name VARCHAR(246) NOT NULL, " +
		"version VARCHAR(246) NOT NULL, " +
		"description VARCHAR(246) NOT NULL, " +
		"status INT(11) NOT NULL, " +
		"virtual_ontology_id VARCHAR(246) NOT NULL, " +
		"format VARCHAR(32) DEFAULT NULL, "+
		"dictionary_id SMALLINT UNSIGNED NOT NULL, " +
		"INDEX X_" + this.getTableSQLName() + "_virtualOntologyID (virtual_ontology_id), " +
		"INDEX X_" + this.getTableSQLName() + "_dictionary_id (dictionary_id)" +
		")ENGINE=MyISAM DEFAULT CHARSET=latin1;";
	}
	
	@Override
	protected void openPreparedStatements() {
		super.openPreparedStatements();
		this.openAddEntryStatement();	
		this.openGetLatestLocalOntologyIDStatement();
		this.openHasNewVersionOfOntologyStatement();
		this.openGetLocalConceptIdByPrefNameAndOntologyId();
		this.openGetAllLocalOntologyIDsStatement();
		this.openDeleteEntriesFromOntologyStatement();
		this.openGetEntryStatement();
		this.openGetAllOntologyBeansStatement();
	}
	
	@Override
	protected void closePreparedStatements() throws SQLException {
		super.closePreparedStatements();
		this.addEntryStatement.close();		
		deleteEntriesFromOntologyStatement.close();
	}
	
	@Override
	protected void openAddEntryStatement(){
		StringBuffer queryb = new StringBuffer();
		queryb.append("INSERT INTO ");
		queryb.append(this.getTableSQLName());
		queryb.append(" (id, local_ontology_id, name, version, description, status, virtual_ontology_id, format, dictionary_id) VALUES (?,?,?,?,?,?,?,?,?);");
		this.addEntryStatement = this.prepareSQLStatement(queryb.toString());
	}
	
	/**
	 * Add a new entry in corresponding(here, obs_ontology) SQL table.
	 * @return True if the entry was added to the SQL table, false if a problem occurred during insertion.
	 */
	public boolean addEntry(OntologyEntry entry){
		boolean inserted = false;
		try {
			this.addEntryStatement.setInt(1, entry.getId());
			this.addEntryStatement.setString(2, entry.getLocalOntologyId());
			this.addEntryStatement.setString(3, entry.getName());
			this.addEntryStatement.setString(4, entry.getVersion());
			this.addEntryStatement.setString(5, entry.getDescription());
			this.addEntryStatement.setInt(6, entry.getStatus());
			this.addEntryStatement.setString(7, entry.getVirtualOntologyId());
			this.addEntryStatement.setString(8, entry.getFormat());
			this.addEntryStatement.setInt(9, entry.getDictionaryId());
			this.executeSQLUpdate(addEntryStatement);
			inserted = true;
		}
		catch (MySQLNonTransientConnectionException e) {
			this.openAddEntryStatement();
			return this.addEntry(entry);
		}
		catch (MySQLIntegrityConstraintViolationException e){
			//logger.error("Table " + this.getTableSQLName() + " already contains an entry for the concept: " + entry.getLocalConceptID() +".");
		}
		catch (SQLException e) {
			logger.error("** PROBLEM ** Cannot add entry "+entry.toString()+" on table " + this.getTableSQLName(), e);
			logger.error(entry.toString());
		}
		return inserted;	
	}
	
	/**
	 * Add a set of OntologyEntry to the table.
	 * 
	 * @param HashSet<OntologyEntry> entries
	 * @return the number of added entries
	 */
	public int addEntries(List<OntologyEntry> entries){
		int nbInserted = 0;			 
		for(OntologyEntry entry: entries){				 
			 if(this.addEntry(entry)){
				 nbInserted++;
			 }
		} 		
		logger.info("Number of ontologies added :" +nbInserted);		
		return nbInserted;
	} 
	
	private void openGetEntryStatement(){
		StringBuffer queryb = new StringBuffer();
		queryb.append("SELECT name, version, description, status, virtual_ontology_id, format, dictionary_id FROM ");
		queryb.append(this.getTableSQLName());
		queryb.append(" WHERE localOntologyID=?;");
		getEntryStatement = this.prepareSQLStatement(queryb.toString());
	} 
	
	
	public OntologyEntry getEntry(String localOntologyID){
		OntologyEntry entry = null;
		try {
			getEntryStatement.setString(1, localOntologyID);
			ResultSet rSet = this.executeSQLQuery(getEntryStatement);
			if(rSet.first()){
				entry = new OntologyEntry(localOntologyID, rSet.getString(1), rSet.getString(2), rSet.getString(3),rSet.getInt(4), rSet.getString(5), rSet.getString(6), rSet.getInt(7));
			}
				
			rSet.close();
		}
		catch (MySQLNonTransientConnectionException e) {
			this.openGetEntryStatement();
			return this.getEntry(localOntologyID);
		}
		catch (SQLException e) {
			logger.error("** PROBLEM ** Cannot get ontology entry from "+this.getTableSQLName()+" with localOntologyID:"+ localOntologyID +". Null returned.", e);
		}
		return entry;
	}
	
	/**************************Methods on ontology Table***************************************/

	private void openGetAllOntologyBeansStatement() {
		StringBuffer queryb = new StringBuffer();
		queryb.append("SELECT local_ontology_id, name, version, virtual_ontology_id FROM ");
		queryb.append(this.getTableSQLName());
		queryb.append(";");
		 
		getAllOntologyBeansStatement = this.prepareSQLStatement(queryb.toString());		
	}
	
	public List<OntologyBean>  getAllOntologyBeans(){	
		List<OntologyBean> ontologyBeans = new ArrayList<OntologyBean>();
		OntologyBean ontologyBean = null;
		try {
			 
			ResultSet rSet = this.executeSQLQuery(getAllOntologyBeansStatement);
			while(rSet.next()){
				ontologyBean = new OntologyBean(rSet.getString(1), rSet.getString(2), rSet.getString(3), rSet.getString(4) );
				ontologyBeans.add(ontologyBean);
			} 	
			rSet.close();
		}
		catch (MySQLNonTransientConnectionException e) {
			this.openGetAllOntologyBeansStatement();
			return this.getAllOntologyBeans();
		}
		catch (SQLException e) {
			logger.error("** PROBLEM ** Cannot get ontology entries from "+this.getTableSQLName()+".Null returned.", e);
		}
		
		return ontologyBeans;
		
	}
	
	/**
	 * 
	 */
	private void openGetLatestLocalOntologyIDStatement() {
		StringBuffer queryb = new StringBuffer();
		queryb.append("SELECT local_ontology_id FROM ");
		queryb.append(this.getTableSQLName());
		queryb.append(" where virtual_ontology_id= ? order by id DESC;");
		 
		getLatestLocalOntologyIDStatement = this.prepareSQLStatement(queryb.toString());		
	}
	
	/**
	 * This method gets latest version of ontology for given virtual ontology id
	 * 
	 * @param virtualOntologyID 
	 * @return String of latest version of ontology.
	 */
	public String getLatestLocalOntologyID(String virtualOntologyID) {
		String localOntologyID= null;
		try {
			ResultSet rSet;			 
			getLatestLocalOntologyIDStatement.setString(1, virtualOntologyID);
			rSet = this.executeSQLQuery(getLatestLocalOntologyIDStatement);
			 
			if(rSet.first()){
				localOntologyID=rSet.getString(1); 
			} 
			
			rSet.close();
		}
		catch (MySQLNonTransientConnectionException e) {
			this.openGetLatestLocalOntologyIDStatement();
			 
			return this.getLatestLocalOntologyID(virtualOntologyID);
		}
		catch (SQLException e) {
			logger.error("** PROBLEM ** Cannot get local ontology ID for "+virtualOntologyID+". Empty set returned.", e);
		}
		return localOntologyID;
	}
	
	private void openHasNewVersionOfOntologyStatement(){
		StringBuffer queryb = new StringBuffer();
		queryb.append("SELECT DISTINCT local_ontology_id, dictionary_id FROM ");
		queryb.append(this.getTableSQLName());
		queryb.append(" OT, ");
		queryb.append(ConceptDao.name());
		queryb.append(" CT, ");
		queryb.append(TermDao.name());
		queryb.append(" TT WHERE TT.concept_id = CT.id AND  CT.ontology_id=OT.id");
		queryb.append(" AND OT.virtual_ontology_id= ? order BY OT.id DESC;");
		 
		hasNewVersionOfOntologyStatement = this.prepareSQLStatement(queryb.toString());	
	}

	/**
	 * Check the new version for given virtualOntologyID present which is not processed(not annotated )
	 * by given resourceID.
	 * 
	 * @param ontoID
	 * @param resourceID
	 * @return
	 */
	public boolean hasNewVersionOfOntology(String virtualOntologyID, String resourceID) {
	 
		int dictionaryID= 0;
		try {
			ResultSet rSet;			 
			hasNewVersionOfOntologyStatement.setString(1, virtualOntologyID);
			rSet = this.executeSQLQuery(hasNewVersionOfOntologyStatement);
			 
			if(rSet.first()){				 
				dictionaryID = rSet.getInt(2);
			} 
			rSet.close();
			 
			if(dictionaryID >0){
				if(dictionaryID > resourceTableDao.getDictionaryId(resourceID)){
					return true;
				}
			} 
		}
		catch (MySQLNonTransientConnectionException e) {
			this.openHasNewVersionOfOntologyStatement();
			 
			return this.hasNewVersionOfOntology(virtualOntologyID, resourceID);
		}
		catch (SQLException e) {
			logger.error("** PROBLEM ** Cannot get local ontology ID for "+virtualOntologyID+". Empty set returned.", e);
		}
		return false;
	
	}

	private void openGetLocalConceptIdByPrefNameAndOntologyId(){
		StringBuffer queryb = new StringBuffer();
		queryb.append("SELECT local_concept_id ");
		queryb.append("FROM ");
		queryb.append(TermDao.name());
		queryb.append(" TT, ");
		queryb.append(ConceptDao.name());
		queryb.append(" CT, ");
		queryb.append(this.getTableSQLName());
		queryb.append(" OT WHERE TT.concept_id= CT.id AND CT.ontology_id=OT.id AND ");
		queryb.append("TT.is_preferred=true AND OT.local_ontology_id=? AND TT.name=?;");		 
		getLocalConceptIdByPrefNameAndOntologyIdStatement = this.prepareSQLStatement(queryb.toString());
	}

	public String getLocalConceptIdByPrefNameAndOntologyId(String localOntologyID, String termName){
		String localConceptID = EMPTY_STRING;
		try {
			getLocalConceptIdByPrefNameAndOntologyIdStatement.setString(1, localOntologyID);
			getLocalConceptIdByPrefNameAndOntologyIdStatement.setString(2, termName);
			ResultSet rSet = this.executeSQLQuery(getLocalConceptIdByPrefNameAndOntologyIdStatement);
			if(rSet.first()){
				localConceptID = rSet.getString(1);
			} 
			rSet.close();
		}
		catch (MySQLNonTransientConnectionException e) {
			this.openGetLocalConceptIdByPrefNameAndOntologyId();
			return this.getLocalConceptIdByPrefNameAndOntologyId(localOntologyID,termName);
		}
		catch (SQLException e) {
			logger.error("** PROBLEM ** Cannot get localConceptID from "+this.getTableSQLName()+" for localConceptID: "+ localConceptID +" and termName: "+termName+". EmptySet returned.", e);
		}
		return localConceptID;
	}
	
	public void  openGetAllLocalOntologyIDsStatement(){
		StringBuffer queryb = new StringBuffer();
		queryb.append("SELECT local_ontology_id FROM ");
		queryb.append(this.getTableSQLName());
		queryb.append("; "); 
		
		getAllLocalOntologyIDsStatement = this.prepareSQLStatement(queryb.toString());		

	}
	
	/**
	 * 
	 * @return
	 */
	public List<String> getAllLocalOntologyIDs(){
		List<String> localOntologyIDs= new ArrayList<String>();		
		
		try {
			ResultSet rSet = this.executeSQLQuery(getAllLocalOntologyIDsStatement);
			while(rSet.next()){
				localOntologyIDs.add(rSet.getString(1));
			} 
			rSet.close();
		}
		catch (MySQLNonTransientConnectionException e) {
			this.openGetAllLocalOntologyIDsStatement();
			return this.getAllLocalOntologyIDs();
		}
		catch (SQLException e) {
			logger.error("** PROBLEM ** Cannot get localConceptID from "+this.getTableSQLName()+". EmptySet returned.", e);
		}
		
		return localOntologyIDs;
	}
	/**
	 * 
	 */
	private void openDeleteEntriesFromOntologyStatement(){
		/*DELETE obs_ontology FROM obs_ontology WHERE obs_ontology.local_ontology_id=?; */
		StringBuffer queryb = new StringBuffer();
		queryb.append("DELETE ");
		queryb.append(this.getTableSQLName());
		queryb.append(" FROM ");
		queryb.append(this.getTableSQLName());		
		queryb.append(" WHERE ");
		queryb.append(this.getTableSQLName());		
		queryb.append(".local_ontology_id=?");
		deleteEntriesFromOntologyStatement = this.prepareSQLStatement(queryb.toString());
	}
	/**
	 * Deletes the rows for the given local_ontology_id.
	 * 
	 * @return true if the rows were successfully removed. 
	 */
	public boolean deleteEntriesFromOntology(String localOntologyID){
		boolean deleted = false;
		try{
			deleteEntriesFromOntologyStatement.setString(1, localOntologyID);
			executeSQLUpdate(deleteEntriesFromOntologyStatement);
			deleted = true;
		}		
		catch (MySQLNonTransientConnectionException e) {
			this.openDeleteEntriesFromOntologyStatement();
			return this.deleteEntriesFromOntology(localOntologyID);
		}
		catch (SQLException e) {
			logger.error("** PROBLEM ** Cannot delete entries from "+this.getTableSQLName()+" for local_ontology_id: "+ localOntologyID+". False returned.", e);
		}
		return deleted;
	}
	
	/**
	 * This method gives Set of all the ontology versions i.e local ontology ids present  in obs_ontology
	 *
	 * @return {@code Set} of local ontology ids
	 */
	public HashSet<String> getLocalOntologyIds(){
		// Query: SELECT DISTINCT local_ontology_id FROM obs_ontology
		HashSet<String> localOntologyIDs = new HashSet<String>();
		StringBuffer queryb = new StringBuffer();
		queryb.append("SELECT DISTINCT local_ontology_id FROM ");
		queryb.append(this.getTableSQLName());
		queryb.append(";");
		try{
			ResultSet rSet = this.executeSQLQuery(queryb.toString());
			while(rSet.next()){
				localOntologyIDs.add(rSet.getString(1));
			}
			rSet.close();
			this.closeTableGenericStatement();
		}		
		catch (SQLException e) {
			logger.error("** PROBLEM ** Cannot get localOntologyIDs from "+this.getTableSQLName()+".", e);
		}
		return localOntologyIDs;
	} 
}

package org.ncbo.resource_access_tools.dao.concept;

import java.io.File;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import org.ncbo.resource_access_tools.dao.obs.AbstractObsDao;
import org.ncbo.resource_access_tools.dao.ontology.OntologyDao;
import org.ncbo.resource_access_tools.util.MessageUtils;

import com.mysql.jdbc.exceptions.MySQLIntegrityConstraintViolationException;
import com.mysql.jdbc.exceptions.MySQLNonTransientConnectionException;
/**
 * This class is a representation for the OBS(slave) DB obs_concept table. The table contains 
 * the following columns:
 * <ul>
 * <li>id INT(11) NOT NULL PRIMARY KEY
   <li>local_concept_id VARCHAR(246) NOT NULL UNIQUE
   <li>ontology_id INT(11) NOT NULL
   <li>is_toplevel TINY NOT NULL
 * </ul>
 * 
 */
public class ConceptDao extends AbstractObsDao {
	
	private static final String TABLE_SUFFIX = MessageUtils.getMessage("obs.concept.table.suffix");
		
	private PreparedStatement addEntryStatement;
	private static PreparedStatement deleteEntriesFromOntologyStatement;
	
	private ConceptDao() {
		super(TABLE_SUFFIX);

	}
	public static String name(){		
		return OBS_PREFIX + TABLE_SUFFIX;
	}
	
	private static class ConceptDaoHolder {
		private final static ConceptDao CONCEPT_DAO_INSTANCE = new ConceptDao();
	}

	/**
	 * Returns a ConceptTable object by creating one if a singleton not already exists.
	 */
	public static ConceptDao getInstance(){
		return ConceptDaoHolder.CONCEPT_DAO_INSTANCE;
	}
	
	@Override
	protected String creationQuery() {
		return "CREATE TABLE " + this.getTableSQLName() +" (" +
		"id INT(11) NOT NULL PRIMARY KEY, " +		
		"local_concept_id VARCHAR(246) NOT NULL UNIQUE, " +
		"ontology_id INT(11) NOT NULL, " +
		"is_toplevel BOOL NOT NULL, " +	 
		"full_id TEXT, " +		 
		"INDEX X_" + this.getTableSQLName() +"_ontology_id (ontology_id), " +
		"INDEX X_" + this.getTableSQLName() +"_isTopLevel (is_toplevel)" +
	")ENGINE=MyISAM DEFAULT CHARSET=latin1 ;";
}
	@Override
	protected void openPreparedStatements() {
		super.openPreparedStatements();
		this.openAddEntryStatement();	
		this.openDeleteEntriesFromOntologyStatement();
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
		queryb.append(" (id, local_concept_id, ontology_id, is_toplevel) VALUES (?,?,?,?);");
		this.addEntryStatement = this.prepareSQLStatement(queryb.toString());
	}
	
	/**
	 * Add a new entry in corresponding(here, obs_concept) SQL table.
	 * @return True if the entry was added to the SQL table, false if a problem occurred during insertion.
	 */
	public boolean addEntry(ConceptEntry entry){
		boolean inserted = false;
		try {
			addEntryStatement.setInt(1, entry.getId());
			addEntryStatement.setString(2, entry.getLocalConceptID());
			addEntryStatement.setInt(3, entry.getOntologyID());
			addEntryStatement.setBoolean(4, entry.isTopLevel());
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
			logger.error("** PROBLEM ** Cannot add an entry on table " + this.getTableSQLName(), e);
			logger.error(entry.toString());
		}
		return inserted;	
	} 	 
	
	/**
	 * Method loads the data entries from given file to concept table
	 * 
	 * @param conceptEntryFile File containing concept table entries.
	 * @return Number of entries populated in concept table.
	 */
	public long populateSlaveConceptTableFromFile(File conceptEntryFile) {
		long nbInserted =0 ;
		
		StringBuffer queryb = new StringBuffer();
		queryb.append("LOAD DATA LOCAL INFILE '");
		queryb.append(conceptEntryFile.getAbsolutePath());
		queryb.append("' IGNORE INTO TABLE ");
		queryb.append(this.getTableSQLName());
		queryb.append(" FIELDS TERMINATED BY '\t' IGNORE 1 LINES"); 
		logger.info("populateSlaveConceptTableFromFile ::::"+queryb.toString());
		try{
			 nbInserted = this.executeSQLUpdate(queryb.toString());			
		} catch (SQLException e) {			 
			logger.error("Problem in populating concept table from file : " + conceptEntryFile.getAbsolutePath(), e);
		} 	
		return nbInserted;
	}
	/**
	 * 
	 */
	private void openDeleteEntriesFromOntologyStatement(){		 
		// Query used :
		// DELETE CT FROM obs_concept CT, obs_ontology OT
		//		WHERE CT.ontology_id = OT.id
		//			AND OT.local_ontology_id = ?; 
		
		StringBuffer queryb = new StringBuffer();
		queryb.append("DELETE CT FROM ");
		queryb.append(this.getTableSQLName());		
		queryb.append(" CT ");
		queryb.append(" WHERE CT.ontology_id in (");
		queryb.append(" SELECT id from  ");
		queryb.append(OntologyDao.name());
		queryb.append(" OT ");
		queryb.append(" WHERE OT.local_ontology_id in ");
		queryb.append(" ( ?  ");
		queryb.append("));");
		
		
		/*queryb.append("DELETE CT FROM ");
		queryb.append(this.getTableSQLName());		
		queryb.append(" CT, ");
		queryb.append(OntologyDao.name());		
		queryb.append(" OT WHERE CT.ontology_id = OT.id AND OT.local_ontology_id = ?;" );*/
		deleteEntriesFromOntologyStatement = this.prepareSQLStatement(queryb.toString());
	}
	
	/**
	 * Deletes the rows for the given local_ontology_id.
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
	 * This class is representation for obs_concept table entry.
	 * @author k.planisamy
	 *
	 */
	public static class ConceptEntry{
		
		private int id;
		private String localConceptID;
		private int ontologyID;
		private boolean isTopLevel;
				
		public ConceptEntry(int id, String localConceptID, int ontologyID,
				boolean isToplevel) {
			this.id = id;
			this.localConceptID = localConceptID;
			this.ontologyID = ontologyID;
			this.isTopLevel = isToplevel;
		}
		
		public int getId() {
			return id;
		}
		public void setId(int id) {
			this.id = id;
		}
		public String getLocalConceptID() {
			return localConceptID;
		}
		public void setLocalConceptID(String localConceptID) {
			this.localConceptID = localConceptID;
		}
		public int getOntologyID() {
			return ontologyID;
		}
		public void setOntologyID(int ontologyID) {
			this.ontologyID = ontologyID;
		}
		
		public boolean isTopLevel() {
			return isTopLevel;
		}
		public void setTopLevel(boolean isTopLevel) {
			this.isTopLevel = isTopLevel;
		}
		public String toString(){
			StringBuffer sb = new StringBuffer();
			sb.append("ConceptEntry: [");
			sb.append(this.localConceptID + ",");
			sb.append(this.localConceptID);
			sb.append(", ");
			sb.append(this.ontologyID);
			sb.append(", ");
			sb.append(this.isTopLevel);
			sb.append("]");
			return sb.toString();
		}		
	}	 
}

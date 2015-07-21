package org.ncbo.resource_access_tools.dao.statistics;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import org.ncbo.resource_access_tools.dao.AbstractObrDao;
import org.ncbo.resource_access_tools.util.MessageUtils;

import com.mysql.jdbc.exceptions.MySQLIntegrityConstraintViolationException;
import com.mysql.jdbc.exceptions.MySQLNonTransientConnectionException;

/**
 * This class is a representation for the the OBR DB OBR_PGDI_CTX table. The table contains 
 * the following columns:
 * 
 * <ul>
 * <li> id                  SMALLINT UNSIGNED  
 * <li> resource_id          INT UNSIGNED NOT NULL 
 * <li>	ontology_id          INT UNSIGNED NOT NULL 
 * <li>	aggregated_annotations  INT UNSIGNED 
 * <li>	mgrep_annotations    INT UNSIGNED 
 * <li>	reported_annotations INT UNSIGNED 
 * <li>	isa_annotations      INT UNSIGNED 
 * <li>	mapping_annotations  INT UNSIGNED 
 * <li>  
 * </ul>
 *  
 * @author kyadav
 * @version OBR_v0.2		
 * @created 01-Dec-2009
 *
 */
public class StatisticsDao extends AbstractObrDao {

	private static final String TABLE_SUFFIX = MessageUtils.getMessage("obr.statistics.table.suffix");

	private static PreparedStatement addEntryStatement;
	private static PreparedStatement getOntolgyIDsForResourceStatement;
	private static PreparedStatement deleteEntryStatement;
	private static PreparedStatement deleteStatisticsForResourceStatement;
	 
		
	private StatisticsDao() {
		super(EMPTY_STRING, TABLE_SUFFIX);
	}

	public static String name(){		
		return OBR_PREFIX + TABLE_SUFFIX;
	}
	
	@Override
	protected String creationQuery(){
		return "CREATE TABLE " + this.getTableSQLName() +" (" +
					"id SMALLINT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY, " +
					"resource_id INT UNSIGNED NOT NULL, " +
					"ontology_id INT UNSIGNED NOT NULL, " +
					"aggregated_annotations BIGINT UNSIGNED," +
					"mgrep_annotations BIGINT UNSIGNED," +
					"reported_annotations BIGINT UNSIGNED," +
					"isa_annotations BIGINT UNSIGNED," +
					"mapping_annotations BIGINT UNSIGNED," +
					"UNIQUE (resource_id, ontology_id), " +					
					"INDEX X_" + this.getTableSQLName() +"_resource_id (resource_id), " +
					"INDEX X_" + this.getTableSQLName() +"_ontology_id (ontology_id)" +
				")ENGINE=MyISAM DEFAULT CHARSET=latin1;";
	}

	@Override
	protected void openPreparedStatements() {
		super.openPreparedStatements();
		this.openAddEntryStatement();
		this.openGetOntolgyIDsForResourceStatement();
		this.openDeleteEntryStatement();
		this.openDeleteStatisticsForResourceStatement(); 
	}

	@Override
	protected void closePreparedStatements() throws SQLException {
		super.closePreparedStatements();
		addEntryStatement.close();
		getOntolgyIDsForResourceStatement.close();
		deleteEntryStatement.close();
		deleteStatisticsForResourceStatement.close();		 
	}

	private static class ContextTableHolder {
		private final static StatisticsDao OBR_STATS_INSTANCE = new StatisticsDao();
	}

	/**
	 * Returns a StatisticsDao object by creating one if a singleton not already exists.
	 */
	public static StatisticsDao getInstance(){
		return ContextTableHolder.OBR_STATS_INSTANCE;
	}

	/****************************************** FUNCTIONS ON THE TABLE ***************************/ 
	
	@Override
	protected void openAddEntryStatement(){
		StringBuffer queryb = new StringBuffer();
		queryb.append("INSERT INTO ");
		queryb.append(this.getTableSQLName());
		queryb.append(" (resource_id, ontology_id, aggregated_annotations, mgrep_annotations, reported_annotations, isa_annotations, mapping_annotations) VALUES (?,?,?,?,?,?,?) ");	
		queryb.append(" ON DUPLICATE KEY UPDATE aggregated_annotations=?, mgrep_annotations= ?, reported_annotations= ?, isa_annotations=?, mapping_annotations=? ;");
		addEntryStatement = this.prepareSQLStatement(queryb.toString());
	}

	/**
	 * Add an new entry in corresponding SQL table.
	 * @return True if the entry was added to the SQL table, false if a problem occurred during insertion.
	 */
	public boolean addEntry(StatisticsEntry entry){
		boolean inserted = false;
		try {
			addEntryStatement.setInt(1, entry.getResourceId());
			addEntryStatement.setInt(2, entry.getOntologyID());
			addEntryStatement.setLong(3, entry.getAggregatedAnnotations());
			addEntryStatement.setLong(4, entry.getMgrepAnnotations());
			addEntryStatement.setLong(5, entry.getReportedAnnotations());
			addEntryStatement.setLong(6, entry.getIsaAnnotations());
			addEntryStatement.setLong(7, entry.getMappingAnnotations());
			
			addEntryStatement.setLong(8, entry.getAggregatedAnnotations());
			addEntryStatement.setLong(9, entry.getMgrepAnnotations());
			addEntryStatement.setLong(10, entry.getReportedAnnotations());
			addEntryStatement.setLong(11, entry.getIsaAnnotations());
			addEntryStatement.setLong(12, entry.getMappingAnnotations());
			 
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
	 * Add a set of StatisticsEntry to the table.
	 * @param HashSet<StatisticsEntry> entries
	 * @return the number of added entries
	 */
	public long addEntries(HashSet<StatisticsEntry> entries){
		long nbInserted = 0;
		for(StatisticsEntry entry: entries){
			if (this.addEntry(entry)){
				nbInserted++;
			}
		}
		return nbInserted;
	} 

	private void openGetOntolgyIDsForResourceStatement(){
		StringBuffer queryb = new StringBuffer();
		queryb.append("SELECT ontology_id FROM ");
		queryb.append(this.getTableSQLName());
		queryb.append(" WHERE resource_id =  ?");	
		getOntolgyIDsForResourceStatement = this.prepareSQLStatement(queryb.toString());
	}
	
	/**
	 * Get list of ontologies used for indexing resource with given resourceID.
	 * 
	 * @param resourceID
	 * @return List of ontology IDs
	 */
	public ArrayList<Integer> getOntolgyIDsForResource(int resourceID){		 
		ArrayList<Integer> ontolgyIDs = new ArrayList<Integer>();
		try {
			getOntolgyIDsForResourceStatement.setInt(1, resourceID); 			 
			ResultSet rSet = this.executeSQLQuery(getOntolgyIDsForResourceStatement);
			
			while(rSet.next()){
				ontolgyIDs.add(rSet.getInt(1));
			}
			 
			rSet.close();
		}
		catch (MySQLNonTransientConnectionException e) {
			this.openGetOntolgyIDsForResourceStatement();
			return this.getOntolgyIDsForResource(resourceID);
		} 
		catch (SQLException e) {			 
			logger.error("** PROBLEM ** Cannot get ontology IDs for resource from table " + this.getTableSQLName(), e);			 
		}
		 
		return ontolgyIDs;
	}
	
	private void openDeleteEntryStatement(){
		StringBuffer queryb = new StringBuffer();
		queryb.append("DELETE ");
		queryb.append(this.getTableSQLName());
		queryb.append(" FROM ");
		queryb.append(this.getTableSQLName());		
		queryb.append(" WHERE resource_id = ? AND ontology_id = ?");	
		deleteEntryStatement = this.prepareSQLStatement(queryb.toString());
	}
	
	/**
	 * Delete entry from STATS table for given resourceID and ontologyID
	 * 
	 * @param resourceID
	 * @param ontologyID
	 * @return
	 */
	public boolean deleteEntry(int resourceID, int ontologyID){
		boolean deleted = false;
		try{
			deleteEntryStatement.setInt(1, resourceID);
			deleteEntryStatement.setInt(2, ontologyID);
			this.executeSQLUpdate(deleteEntryStatement);
			deleted = true;
		}		
		catch (MySQLNonTransientConnectionException e) {
			this.openDeleteEntryStatement();
			return this.deleteEntry(resourceID, ontologyID);
		}
		catch (SQLException e) {
			logger.error("** PROBLEM ** Cannot delete entries from "+this.getTableSQLName()+" for resourceID: " +resourceID + " ontologyID: "+ ontologyID+". False returned.", e);
		}
		return deleted;
	}
	
	private void openDeleteStatisticsForResourceStatement(){
		StringBuffer queryb = new StringBuffer();
		queryb.append("DELETE ");
		queryb.append(this.getTableSQLName());
		queryb.append(" FROM ");
		queryb.append(this.getTableSQLName());	
		queryb.append(" ");
		queryb.append(" WHERE resource_id= (SELECT id FROM ");
		queryb.append(resourceTableDao.getTableSQLName());
		queryb.append("  WHERE resource_id= ?);"); 	
		
		deleteStatisticsForResourceStatement = this.prepareSQLStatement(queryb.toString());
	}
	
	/**
	 * Delete entry from STATS table for given resourceID and ontologyID
	 * 
	 * @param resourceID
	 * @param ontologyID
	 * @return
	 */
	public boolean deleteStatisticsForResource(String resourceID){
		boolean deleted = false;
		try{
			deleteStatisticsForResourceStatement.setString(1, resourceID);			 
			this.executeSQLUpdate(deleteStatisticsForResourceStatement);
			deleted = true;
		}		
		catch (MySQLNonTransientConnectionException e) {
			this.openDeleteStatisticsForResourceStatement();
			return this.deleteStatisticsForResource(resourceID );
		}
		catch (SQLException e) {
			logger.error("** PROBLEM ** Cannot delete entries from "+this.getTableSQLName()+" for resourceID: " +resourceID + ". False returned.", e);
		}
		return deleted;
	} 
	
	/**
	 * Deletes the rows for the given list of local_ontology_id.
	 * 
	 * @return true if the rows were successfully removed. 
	 */
	public boolean deleteEntriesFromOntologies(List<String> localOntologyIDs){
		boolean deleted = false;
		
		if(localOntologyIDs== null ||localOntologyIDs.size()==0 ){
			return deleted;
		}
		
		try{
			/*
			DELETE STAT FROM obs_statistics STAT,obs_ontology OT
		 	WHERE STAT.ontology_id = OT.id and OT.local_ontology_id IN(?, ?, ?);
		    */
			StringBuffer queryb = new StringBuffer();
			queryb.append("DELETE STAT FROM ");
			queryb.append(this.getTableSQLName());			
			queryb.append(" STAT, ");
			queryb.append(ontologyDao.getMemoryTableSQLName());
			queryb.append(" OT WHERE STAT.ontology_id = OT.id AND OT.local_ontology_id IN ( ");			
			for (String localOntologyID : localOntologyIDs) {
				queryb.append(localOntologyID);
				queryb.append(", ");
			}
			queryb.delete(queryb.length()-2, queryb.length());
			queryb.append(");");
			
			executeSQLUpdate(queryb.toString());
			
			deleted = true;
		}		
		catch (MySQLNonTransientConnectionException e) {
		 
			return this.deleteEntriesFromOntologies(localOntologyIDs);
		}
		catch (SQLException e) {
			logger.error("** PROBLEM ** Cannot delete entries from "+this.getTableSQLName()+" for ontology ids. False returned.", e);
		}
		return deleted;
	}
	/********************************* ENTRY CLASS *****************************************************/

	/**
	 * This class is a representation for a OBR_STATS table entry.
	 * 
	 * @author kyadav
	 * @version  	
	 * @created 01-Dec-2008
	 */
	public static class StatisticsEntry {

		private int resourceID;
		private int ontologyID;
		private long aggregatedAnnotations;
		private long mgrepAnnotations;
		private long reportedAnnotations;
		private long isaAnnotations;
		private long mappingAnnotations;
	 
				
		public StatisticsEntry(int resourceID, int ontologyID,
				long aggregatedAnnotations, long mgrepAnnotations,
				long reportedAnnotations, long isaAnnotations,
				long mappingAnnotations) {
			super();
			this.resourceID = resourceID;
			this.ontologyID = ontologyID;
			this.aggregatedAnnotations = aggregatedAnnotations;
			this.mgrepAnnotations = mgrepAnnotations;
			this.reportedAnnotations = reportedAnnotations;
			this.isaAnnotations = isaAnnotations;
			this.mappingAnnotations = mappingAnnotations;
		} 

		

		/**
		 * @return the resourceID
		 */
		public int getResourceId() {
			return resourceID;
		}



		/**
		 * @return the ontologyID
		 */
		public int getOntologyID() {
			return ontologyID;
		} 

		/**
		 * 
		 * @return aggregatedAnnotations
		 */
		public long getAggregatedAnnotations() {
			return aggregatedAnnotations;
		}



		/**
		 * @return the mgrepAnnotations
		 */
		public long getMgrepAnnotations() {
			return mgrepAnnotations;
		}



		/**
		 * @return the reportedAnnotations
		 */
		public long getReportedAnnotations() {
			return reportedAnnotations;
		}



		/**
		 * @return the isaAnnotations
		 */
		public long getIsaAnnotations() {
			return isaAnnotations;
		}



		/**
		 * @return the mappingAnnotations
		 */
		public long getMappingAnnotations() {
			return mappingAnnotations;
		}



		public String toString(){
			StringBuffer sb = new StringBuffer();
			sb.append("StatisticsEntry: [");
			sb.append(this.resourceID);
			sb.append(", ");
			sb.append(this.ontologyID);
			sb.append(", ");
			sb.append(this.aggregatedAnnotations);
			sb.append(", ");
			sb.append(this.mgrepAnnotations);
			sb.append(", ");
			sb.append(this.reportedAnnotations);
			sb.append(", ");
			sb.append(this.isaAnnotations);			 
			sb.append(", ");
			sb.append(this.mappingAnnotations);
			sb.append("]");
			return sb.toString();
		}
	}
}

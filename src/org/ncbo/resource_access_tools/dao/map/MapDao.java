package org.ncbo.resource_access_tools.dao.map;

import java.io.File;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import org.ncbo.resource_access_tools.dao.concept.ConceptDao;
import org.ncbo.resource_access_tools.dao.obs.AbstractObsDao;
import org.ncbo.resource_access_tools.dao.ontology.OntologyDao;
import org.ncbo.resource_access_tools.util.MessageUtils;

import com.mysql.jdbc.exceptions.MySQLNonTransientConnectionException;
/**
 * This class is a representation for the OBS(slave) DB obs_map table. The table contains 
 * the following columns:
 * <ul> 
 * <li>id INT(11) NOT NULL PRIMARY KEY
   <li>concept_id INT(11) NOT NULL
   <li>mapped_concept_id INT(11) NOT NULL
   <li>mapping_type VARCHAR(246) NOT NULL
 * </ul>
 * 
 */
public class MapDao extends AbstractObsDao{
	
	private static final String TABLE_SUFFIX = MessageUtils.getMessage("obs.map.table.suffix");
 
	private PreparedStatement addEntryStatement;
	private static PreparedStatement deleteEntriesFromOntologyStatement;
    private MapppingTypeDao mapppingTypeDao= MapppingTypeDao.getInstance();
	
	private MapDao() {
		super(TABLE_SUFFIX);

	}
	
	private static class MapDaoHolder {
		private final static MapDao MAP_DAO_INSTANCE = new MapDao();
	}

	/**
	 * Returns a ConceptTable object by creating one if a singleton not already exists.
	 */
	public static MapDao getInstance(){
		return MapDaoHolder.MAP_DAO_INSTANCE;
	}
	
	public static String name(String resourceID){		
		return OBS_PREFIX + TABLE_SUFFIX;
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
	protected void openAddEntryStatement() {
		StringBuffer queryb = new StringBuffer();
		queryb.append("INSERT INTO ");
		queryb.append(this.getTableSQLName());
		queryb.append(" (id, concept_id, mapped_concept_id, mapping_type) VALUES (?,?,?,?);");
		this.addEntryStatement = this.prepareSQLStatement(queryb.toString());
	}

	@Override
	protected String creationQuery() {
		return "CREATE TABLE " + this.getTableSQLName() +" (" +
		"id INT(11) NOT NULL PRIMARY KEY, " +
		"concept_id INT(11) NOT NULL, " +
		"mapped_concept_id INT(11) NOT NULL, " +
		"mapping_type VARCHAR(30) NOT NULL, " +
		//"UNIQUE (concept_id, mapped_concept_id ), " +	 
		"INDEX X_" + this.getTableSQLName() +"_concept_id (concept_id), " +
		"INDEX X_" + this.getTableSQLName() +"_mapped_concept_id (mapped_concept_id), " +
		"INDEX X_" + this.getTableSQLName() +"_mappingType (mapping_type(10))" +
		") ENGINE=MyISAM DEFAULT CHARSET=latin1; ";
	}

	/**
	 * Add a new entry in corresponding(here, obs_map) SQL table.
	 * @return True if the entry was added to the SQL table, false if a problem occurred during insertion.
	 */
	public boolean addEntry(MapEntry entry){
		boolean inserted = false;
		try {
			addEntryStatement.setInt(1, entry.getId());
			addEntryStatement.setInt(2, entry.getConceptID());
			addEntryStatement.setInt(3, entry.getMappedConceptID());
			addEntryStatement.setString(4, entry.getMappingType());
			this.executeSQLUpdate(addEntryStatement);
			inserted = true;
		}
		catch (MySQLNonTransientConnectionException e) {
			this.openAddEntryStatement();
			return this.addEntry(entry);
		}
		catch (SQLException e) {
			logger.error("** PROBLEM ** Cannot add an entry on table " + this.getTableSQLName(), e);
			logger.error(entry.toString());
		}
		return inserted;	
	} 
	
	public long populateMappingTypeTable() {
		StringBuffer queryb = new StringBuffer();
		queryb.append("INSERT IGNORE INTO ");
		queryb.append(mapppingTypeDao.getTableSQLName());
		queryb.append(" (mapping_type) SELECT DISTINCT mapping_type FROM ");
		queryb.append(this.getTableSQLName());
		queryb.append("; ");		
		long nbInserted =0 ;		
		try{
			 nbInserted = this.executeSQLUpdate(queryb.toString());
			
		} catch (SQLException e) {			 
			logger.error("Problem in populating mapping type table" , e);
		}	
		return nbInserted;
	} 
	
	/**
	 * Method loads the data entries from given file to map table.
	 * 
	 * @param mappingEntryFile File containing term table entries.
	 * @return Number of entries populated in mapping table.
	 */
	public long populateSlaveMappingTableFromFile(File mappingEntryFile) {
		StringBuffer queryb = new StringBuffer();
		queryb.append("LOAD DATA LOCAL INFILE '");
		queryb.append(mappingEntryFile.getAbsolutePath());
		queryb.append("' IGNORE INTO TABLE ");
		queryb.append(this.getTableSQLName());
		queryb.append(" FIELDS TERMINATED BY '\t' IGNORE 1 LINES");
		
		long nbInserted =0 ;
		
		try{
			 nbInserted = this.executeSQLUpdate(queryb.toString());
			
		} catch (SQLException e) {			 
			logger.error("Problem in populating map table from file : " + mappingEntryFile.getAbsolutePath(), e);
		}	
		return nbInserted;
	}
	
	private void openDeleteEntriesFromOntologyStatement(){
		// Query Used :
		//	DELETE MAPT FROM obs_map MAPT, obs_concept CT, obs_ontology OT
		//		WHERE MAPT.conept_id = CT.id
		//			AND CT.ontology_id = OT.id
		//			AND OT.local_ontology_id = ?;		
		StringBuffer queryb = new StringBuffer();
		queryb.append("DELETE MAPT FROM ");
		queryb.append(this.getTableSQLName());		
		queryb.append(" MAPT ");
		queryb.append(" WHERE MAPT.concept_id in (");
		queryb.append(" SELECT id from  ");
		queryb.append(ConceptDao.name( ));	
		queryb.append(" CT ");
		queryb.append(" WHERE CT.ontology_id in (");
		queryb.append(" SELECT id from  ");
		queryb.append(OntologyDao.name());
		queryb.append(" OT ");
		queryb.append(" WHERE OT.local_ontology_id in ");
		queryb.append(" ( ?  ");
		queryb.append(")));");
		
		/*queryb.append("DELETE MAPT FROM ");
		queryb.append(this.getTableSQLName());		
		queryb.append(" MAPT, ");
		queryb.append(ConceptDao.name( ));	
		queryb.append(" CT, ");
		queryb.append(OntologyDao.name());
		queryb.append(" OT ");
		queryb.append(" WHERE MAPT.concept_id = CT.id AND CT.ontology_id = OT.id AND OT.local_ontology_id = ?");
 	*/
		deleteEntriesFromOntologyStatement = this.prepareSQLStatement(queryb.toString());
	}
	/**
	 * Deletes the rows for the given local_ontology_id from map table.
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
	 * This class is representation for obs_map table entry.
	 * @author 
	 *
	 */
	public static class MapEntry{

		private int id;
		private int conceptID;
		private int mappedConceptID;
		private String mappingType;


		public MapEntry(int id, int conceptID, int mappedConceptID,
				String mappingType) {
			this.id = id;
			this.conceptID = conceptID;
			this.mappedConceptID = mappedConceptID;
			this.mappingType = mappingType;
		}

		public int getId() {
			return id;
		}
		public void setId(int id) {
			this.id = id;
		}
		public int getConceptID() {
			return conceptID;
		}
		public void setConceptID(int conceptID) {
			this.conceptID = conceptID;
		}
		public int getMappedConceptID() {
			return mappedConceptID;
		}
		public void setMappedConceptID(int mappedConceptID) {
			this.mappedConceptID = mappedConceptID;
		}
		public String getMappingType() {
			return mappingType;
		}
		public void setMappingType(String mappingType) {
			this.mappingType = mappingType;
		}

		public String toString(){
			StringBuffer sb = new StringBuffer();
			sb.append("MapEntry: [");
			sb.append(this.id);
			sb.append(", ");			
			sb.append(this.conceptID);
			sb.append(" maps to ");
			sb.append(this.mappedConceptID);
			sb.append(" (");
			sb.append(this.mappingType);
			sb.append(")");
			return sb.toString();
		}
	} 
	
}

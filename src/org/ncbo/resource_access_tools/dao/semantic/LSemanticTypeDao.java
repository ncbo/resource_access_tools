package org.ncbo.resource_access_tools.dao.semantic;

import java.io.File;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import org.ncbo.resource_access_tools.dao.obs.AbstractObsDao;
import org.ncbo.resource_access_tools.util.MessageUtils;
/**
 * This class is a representation for the OBS(slave) DB obs_sematic_type table. The table contains 
 * the following columns:
 * <ul>
 * <li>id INT(11) NOT NULL PRIMARY KEY
 *  <li>local_concept_id VARCHAR(246) NOT NULL UNIQUE
 *  <li>ontology_id INT(11) NOT NULL
 *  <li>is_toplevel TINY NOT NULL
 * </ul>
 * 
 */
public class LSemanticTypeDao extends AbstractObsDao {
	
	private static final String TABLE_SUFFIX = MessageUtils.getMessage("obs.l.semantic.table.suffix");
		
	private PreparedStatement addEntryStatement; 
	
	private LSemanticTypeDao() {
		super(TABLE_SUFFIX);

	}
	public static String name(){		
		return OBS_PREFIX + TABLE_SUFFIX;
	}
	
	private static class LSemanticTypeDaoHolder {
		private final static LSemanticTypeDao L_SEMANTIC_DAO_INSTANCE = new LSemanticTypeDao();
	}

	/**
	 * Returns a ConceptTable object by creating one if a singleton not already exists.
	 */
	public static LSemanticTypeDao getInstance(){
		return LSemanticTypeDaoHolder.L_SEMANTIC_DAO_INSTANCE;
	}
	
	@Override
	protected String creationQuery() {
		return "CREATE TABLE " + this.getTableSQLName() +" (" +
				"id INT(11) NOT NULL PRIMARY KEY, " +	
				"semantic_type varchar(32) NOT NULL, "+
				"description varchar(256) DEFAULT NULL" +		 
				")ENGINE=MyISAM DEFAULT CHARSET=latin1 ;";
}
	@Override
	protected void openPreparedStatements() {
		super.openPreparedStatements();
		this.openAddEntryStatement();	 
	}
	
	@Override
	protected void closePreparedStatements() throws SQLException {
		super.closePreparedStatements();
		this.addEntryStatement.close();		 
	}
	
	@Override
	protected void openAddEntryStatement(){
		StringBuffer queryb = new StringBuffer();
		queryb.append("INSERT INTO ");
		queryb.append(this.getTableSQLName());
		queryb.append(" (id, semantic_type, description ) VALUES (?,?,?);");
		this.addEntryStatement = this.prepareSQLStatement(queryb.toString());
	} 
	/**
	 * Method loads the data entries from given file to semantic Type table
	 * 
	 * @param conceptEntryFile File containing concept table entries.
	 * @return Number of entries populated in concept table.
	 */
	public long populateSlaveLSemanticTypeTableFromFile(File lSemanticTypeEntryFile) {
		long nbInserted =0 ;
		
		StringBuffer queryb = new StringBuffer();
		queryb.append("LOAD DATA LOCAL INFILE '");
		queryb.append(lSemanticTypeEntryFile.getAbsolutePath());
		queryb.append("' IGNORE INTO TABLE ");
		queryb.append(this.getTableSQLName());
		queryb.append(" FIELDS TERMINATED BY '\t' IGNORE 1 LINES"); 
		try{
			 nbInserted = this.executeSQLUpdate(queryb.toString());			
		} catch (SQLException e) {			 
			logger.error("Problem in populating concept table from file : " + lSemanticTypeEntryFile.getAbsolutePath(), e);
		} 	
		return nbInserted;
	} 
	 
}

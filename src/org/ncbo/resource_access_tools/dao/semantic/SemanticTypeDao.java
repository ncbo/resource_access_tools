package org.ncbo.resource_access_tools.dao.semantic;

import java.io.File;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import org.ncbo.resource_access_tools.dao.concept.ConceptDao;
import org.ncbo.resource_access_tools.dao.obs.AbstractObsDao;
import org.ncbo.resource_access_tools.dao.ontology.OntologyDao;
import org.ncbo.resource_access_tools.util.MessageUtils;

import com.mysql.jdbc.exceptions.MySQLNonTransientConnectionException;
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
public class SemanticTypeDao extends AbstractObsDao {
	
	private static final String TABLE_SUFFIX = MessageUtils.getMessage("obs.semantic.table.suffix");
		
	private PreparedStatement addEntryStatement;
	private static PreparedStatement deleteEntriesFromOntologyStatement;
	
	private SemanticTypeDao() {
		super(TABLE_SUFFIX);

	}
	public static String name(){		
		return OBS_PREFIX + TABLE_SUFFIX;
	}
	
	private static class SemanticTypeDaoHolder {
		private final static SemanticTypeDao SEMANTIC_DAO_INSTANCE = new SemanticTypeDao();
	}

	/**
	 * Returns a ConceptTable object by creating one if a singleton not already exists.
	 */
	public static SemanticTypeDao getInstance(){
		return SemanticTypeDaoHolder.SEMANTIC_DAO_INSTANCE;
	}
	
	@Override
	protected String creationQuery() {
		return "CREATE TABLE " + this.getTableSQLName() +" (" +
		"id INT(11) NOT NULL PRIMARY KEY, " +			 
		"concept_id INT(11) NOT NULL, " +
		"semantic_type_id INT(11) NOT NULL, " +		 
		"INDEX X_" + this.getTableSQLName() +"_concept_id (concept_id), " +
		"INDEX X_" + this.getTableSQLName() +"_semantic_type_id (semantic_type_id)" +		 
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
		queryb.append(" (id, concept_id, semantic_type_id ) VALUES (?,?,?);");
		this.addEntryStatement = this.prepareSQLStatement(queryb.toString());
	} 
	/**
	 * Method loads the data entries from given file to semantic Type table
	 * 
	 * @param conceptEntryFile File containing concept table entries.
	 * @return Number of entries populated in concept table.
	 */
	public long populateSlaveSemanticTypeTableFromFile(File semanticTypeEntryFile) {
		long nbInserted =0 ;
		
		StringBuffer queryb = new StringBuffer();
		queryb.append("LOAD DATA LOCAL INFILE '");
		queryb.append(semanticTypeEntryFile.getAbsolutePath());
		queryb.append("' IGNORE INTO TABLE ");
		queryb.append(this.getTableSQLName());
		queryb.append(" FIELDS TERMINATED BY '\t' IGNORE 1 LINES"); 
		try{
			 nbInserted = this.executeSQLUpdate(queryb.toString());			
		} catch (SQLException e) {			 
			logger.error("Problem in populating concept table from file : " + semanticTypeEntryFile.getAbsolutePath(), e);
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
		queryb.append("DELETE ST FROM ");
		queryb.append(this.getTableSQLName());		
		queryb.append(" ST ");
		queryb.append(" WHERE ST.concept_id in (");
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
		
		
		
		
		/*queryb.append("DELETE ST FROM ");
		queryb.append(this.getTableSQLName());		
		queryb.append(" ST, ");
		queryb.append(ConceptDao.name( ));	
		queryb.append(" CT, ");
		queryb.append(OntologyDao.name());
		queryb.append(" OT ");
		queryb.append(" WHERE ST.concept_id = CT.id AND CT.ontology_id = OT.id AND OT.local_ontology_id = ?");*/
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
	
	 
}

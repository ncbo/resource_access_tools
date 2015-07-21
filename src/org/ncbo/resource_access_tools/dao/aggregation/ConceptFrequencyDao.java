package org.ncbo.resource_access_tools.dao.aggregation;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;

import org.ncbo.resource_access_tools.dao.AbstractObrDao;
import org.ncbo.resource_access_tools.dao.element.ElementDao;
import org.ncbo.resource_access_tools.util.MessageUtils;

import com.mysql.jdbc.exceptions.MySQLNonTransientConnectionException;

/**
 * This class is a representation for the the OBR DB obr_xx_aggregation table. The table contains 
 * the following columns:
 * 
 * <ul>
 * <li> element_id  	            		INT UNSIGNED NOT NULL FOREIGN KEY,
 * <li> concept_id							INT UNSIGNED NOT NULL FOREIGN KEY,
 * <li> score  						        FLOAT,
 * </ul>
 *  
 * @author Adrien Coulet, Clement Jonquet, Kuladip Yadav
 * @version OBR_v0.2		
 * @created 12-Nov-2008
 *
 */
public class ConceptFrequencyDao extends AbstractObrDao {

	private static final String TABLE_SUFFIX = MessageUtils.getMessage("obr.concept.frequency.table.suffix"); // element Index Table
	
	private PreparedStatement addEntryStatement;	 
	private PreparedStatement deleteEntriesFromOntologyStatement;
	 
	/**
	 * Creates a new ConceptFrequencyDao with a given resourceID.
	 * The suffix that will be added for Aggregation table is "_aggregation".
	 */
	public ConceptFrequencyDao(String resourceID) {
		super(resourceID, TABLE_SUFFIX);
	}

	/**
	 * Returns the SQL table name for a given resourceID 
	 */
	public static String name(String resourceID){
		return OBR_PREFIX + resourceID.toLowerCase() + TABLE_SUFFIX;
	}
	
	@Override
	protected String creationQuery(){
		return "CREATE TABLE " + getTableSQLName() +" (" +
					"id INT(11) UNSIGNED NOT NULL PRIMARY KEY, " +
					"counts BIGINT UNSIGNED NOT NULL, " +
					"score FLOAT, " +	
					"INDEX X_" + this.getTableSQLName() +"_counts USING BTREE(counts), " +	
					"INDEX X_" + this.getTableSQLName() +"_score USING BTREE(score) " +	
				")ENGINE=MyISAM DEFAULT CHARSET=latin1;";
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
		this.deleteEntriesFromOntologyStatement.close();		 
	}
	
	/****************************************** FUNCTIONS ON THE TABLE ***************************/ 

	@Override
	protected void openAddEntryStatement(){
		StringBuffer queryb = new StringBuffer();
		queryb.append("INSERT INTO ");
		queryb.append(this.getTableSQLName());
		queryb.append(" (element_id, concept_id, score) VALUES (");
			// sub query to get the elementID from the localElementID
			queryb.append("(SELECT id FROM ");
			queryb.append(ElementDao.name(this.resourceID));
			queryb.append(" WHERE local_element_id=?)");
		queryb.append("	,");
			// sub query to get the conceptID from the localConceptID
			queryb.append("(SELECT id FROM ");
			queryb.append(conceptDao.getTableSQLName());
			queryb.append(" WHERE local_concept_id=?)");
		queryb.append("	,?);");
		this.addEntryStatement = this.prepareSQLStatement(queryb.toString());
	}
 

	// ********************************* AGGREGATION FUNCTIONS  *****************************************************/
	
	/**
	 * This method calculates concept frequency from aggreation table
	 * 
	 * Return number of entries added
	 *  
	 */
	public long calulateConceptFrequncy(){		 
		StringBuffer queryb = new StringBuffer();
		queryb.append("INSERT INTO ");
		queryb.append(this.getTableSQLName());
		queryb.append(" (id, counts, score) ");		 
		// sub query to get the elementID from the localElementID
		queryb.append("SELECT concept_id, COUNT(element_id), SUM(score) FROM ");
		queryb.append(AggregationDao.name(this.resourceID));
		queryb.append(" GROUP by concept_id;");
		
		StringBuffer truncateQuery = new StringBuffer();
		truncateQuery.append("TRUNCATE TABLE ");
		truncateQuery.append(this.getTableSQLName());
		truncateQuery.append(";");
		 
		try{
			this.executeSQLUpdate(truncateQuery.toString());
			this.executeSQLUpdate(queryb.toString());
			}
		catch(SQLException e){
			logger.error("** PROBLEM ** Problem in processing concept frequency ", e);
		} 
		 
		return this.numberOfEntry();
	} 

	/**
	 * Deletes the rows for indexing done with a concept in the given list of localOntologyIDs.
	 * 
	 * @param {@code List} of local ontology ids
	 * @return True if the rows were successfully removed. 
	 */
	public boolean deleteEntriesFromOntologies(List<String> localOntologyIDs){		
		boolean deleted = false;
		StringBuffer queryb = new StringBuffer();
		
		/*queryb.append("DELETE CF FROM ");
		queryb.append(this.getTableSQLName());		
		queryb.append(" CF, ");
		queryb.append(conceptDao.getMemoryTableSQLName());	
		queryb.append(" CT, ");
		queryb.append(ontologyDao.getMemoryTableSQLName());
		queryb.append(" OT ");
		queryb.append(" WHERE CF.id = CT.id AND CT.ontology_id = OT.id AND OT.local_ontology_id IN (");
		
		for (String localOntologyID : localOntologyIDs) {
			queryb.append("'");
			queryb.append(localOntologyID);
			queryb.append("', ");
		}
		queryb.delete(queryb.length()-2, queryb.length());
		queryb.append(");");*/
		
		

		queryb.append("DELETE CF FROM ");
		queryb.append(this.getTableSQLName());
		queryb.append(" CF ");
		queryb.append(" WHERE CF.id IN ");
		queryb.append(" ( SELECT id FROM  ");
		queryb.append(conceptDao.getMemoryTableSQLName());
		queryb.append(" CT ");
		queryb.append(" WHERE CT.ontology_id IN ");
		queryb.append(" ( SELECT id FROM  ");
		queryb.append(ontologyDao.getMemoryTableSQLName());
		queryb.append(" OT ");
		queryb.append(" WHERE OT.local_ontology_id IN ( "); 
		
		for (String localOntologyID : localOntologyIDs) {
			queryb.append("'");
			queryb.append(localOntologyID);
			queryb.append("', ");
		}
		
		queryb.delete(queryb.length()-2, queryb.length());
		queryb.append(")));");
		
		try{			 
			this.executeSQLUpdate(queryb.toString() );
			deleted = true;
		}		
		catch (MySQLNonTransientConnectionException e) {			 
			return this.deleteEntriesFromOntologies(localOntologyIDs);
		}
		catch (SQLException e) {
			logger.error("** PROBLEM ** Cannot delete entries from "+this.getTableSQLName()+" for localOntologyIDs: "+ localOntologyIDs+". False returned.", e);
		}
		return deleted;
	}	
	
		
	// ********************************* ENTRY CLASS *****************************************************/
	
	/**
	 * This class is a representation for a obr_xx_aggregation table entry.
	 * 
	 * @author Adrien Coulet, Clement Jonquet
	 * @version OBR_v0.2		
	 * @created 12-Nov-2008

	 */
	public static class ConceptFrequencyEntry {

		private Integer conceptID;
		private Long count;
		private float score;
		
		public ConceptFrequencyEntry(Integer conceptID, Long count, float score) {
			super();			 
			this.conceptID = conceptID;
			this.count = count;
			this.score = score;
		} 
		
		public Integer getConceptID() {
			return conceptID;
		} 

		public Long getCount() {
			return count;
		} 
		
		public float getScore() {
			return score;
		} 
		
		public String toString(){
			StringBuffer sb = new StringBuffer();
			sb.append("ConceptFrequencyEntry: [");
			sb.append(this.conceptID);
			sb.append(", ");
			sb.append(this.count);
			sb.append(", ");			
			sb.append(this.score);			
			sb.append("]");
			return sb.toString();
		}
	}
}

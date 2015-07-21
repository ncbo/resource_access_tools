package org.ncbo.resource_access_tools.dao.annotation.expanded;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import org.ncbo.resource_access_tools.dao.AbstractObrDao;
import org.ncbo.resource_access_tools.dao.concept.ConceptDao;
import org.ncbo.resource_access_tools.dao.ontology.OntologyDao;

import com.mysql.jdbc.exceptions.MySQLNonTransientConnectionException;

/**
 * This class is a representation for the the OBR DB OBR_XX_EAT table. The table contains 
 * the following columns:
 * 
 * <ul>
 * <li> id 			            		BIGINT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY,
 * <li> elementID  	            			INT UNSIGNED NOT NULL FOREIGN KEY,
 * <li> conceptID							INT UNSIGNED NOT NULL FOREIGN KEY,
 * <li> contextID							SMALLINT UNSIGNED NOT NULL FOREIGN KEY,
 * <li> dictionaryID						SMALLINT UNSIGNED NOT NULL FOREIGN KEY,
 * <li> childConceptID						INT UNSIGNED FOREIGN KEY,
 * <li> level								SMALLINT UNSIGNED,	
 * <li> mappedConceptID						INT UNSIGNED FOREIGN KEY,
 * <li> mappingType							VARVHAR(20),  
 * <li> distantConceptID					INT UNSIGNED FOREIGN KEY,
 * <li> distance							SMALLINT UNSIGNED,
 * <li> indexingDone						BOOL     
 * </ul>
 *  
 * @author Adrien Coulet, Clement Jonquet
 * @version OBR_v0.2		
 * @created 13-Nov-2008
 *
 */
public abstract class AbstractExpandedAnnotationDao extends AbstractObrDao {
  
	private PreparedStatement deleteEntriesFromOntologyStatement;
	
	/**
	 * Creates a new ExpandedAnnotationTable with a given resourceID.
	 * The suffix that will be added for AnnotationTable is "_EAT".
	 */
	public AbstractExpandedAnnotationDao(String resourceID, String tableSuffix) {
		super(resourceID, tableSuffix);
	} 
	
	public abstract String getIndexCreationQuery(); 
	
	@Override
	protected void openPreparedStatements() {
		super.openPreparedStatements();		  
		this.openDeleteEntriesFromOntologyStatement();
	}

	@Override
	protected void closePreparedStatements() throws SQLException {
		super.closePreparedStatements();		  
		this.deleteEntriesFromOntologyStatement.close();
	}
	
	/****************************************** FUNCTIONS ON THE TABLE ***************************/ 

	//********************************* DELETE FUNCTIONS *****************************************************/
	
	/**
	 * Selecting the ontology id from OBS_OT table given local ontology id.
	 * Selecting the concept id from OBS_CT table given ontology id.
	 * Deleting concept id from OBR_EAT table given concept id. 
	 */
	private void openDeleteEntriesFromOntologyStatement(){
		// Query Used :
		//	DELETE EAT FROM obr_tr_expanded_annotation EAT, obs_concept CT, obs_ontology OT
		//		WHERE EAT.conept_id = CT.id
		//			AND CT.ontology_id = OT.id
		//			AND OT.local_ontology_id = ?;		
		StringBuffer queryb = new StringBuffer();
		queryb.append("DELETE EAT FROM ");
		queryb.append(this.getTableSQLName());		
		queryb.append(" EAT, ");
		queryb.append(ConceptDao.name( ));	
		queryb.append(" CT, ");
		queryb.append(OntologyDao.name());
		queryb.append(" OT ");
		queryb.append(" WHERE EAT.concept_id = CT.id AND CT.ontology_id = OT.id AND OT.local_ontology_id = ?");
		this.deleteEntriesFromOntologyStatement = this.prepareSQLStatement(queryb.toString());
	}
		
	/**
	 * Deletes the rows corresponding to annotations done with a concept in the given localOntologyID.
	 * @return True if the rows were successfully removed. 
	 */
	public boolean deleteEntriesFromOntology(String localOntologyID){
		boolean deleted = false;
		try{
			this.deleteEntriesFromOntologyStatement.setString(1, localOntologyID);
			this.executeSQLUpdate(this.deleteEntriesFromOntologyStatement);
			deleted = true;
		}		
		catch (MySQLNonTransientConnectionException e) {
			this.openDeleteEntriesFromOntologyStatement();
			return this.deleteEntriesFromOntology(localOntologyID);
		}
		catch (SQLException e) {
			logger.error("** PROBLEM ** Cannot delete entries from "+this.getTableSQLName()+" for localOntologyID: "+ localOntologyID+". False returned.", e);
		}
		return deleted;
	}
	
	/**
	 * Deletes the rows corresponding to expanded annotations done with a concept in the given list of localOntologyIDs.
	 * 
	 * @param {@code List} of local ontology ids
	 * @return True if the rows were successfully removed. 
	 */
	public boolean deleteEntriesFromOntologies(List<String> localOntologyIDs){		
		boolean deleted = false;
		StringBuffer queryb = new StringBuffer();
		queryb.append("DELETE EAT FROM ");
		queryb.append(this.getTableSQLName());		
		queryb.append(" EAT, ");
		queryb.append(ConceptDao.name( ));	
		queryb.append(" CT, ");
		queryb.append(OntologyDao.name());
		queryb.append(" OT ");
		queryb.append(" WHERE EAT.concept_id = CT.id AND CT.ontology_id = OT.id AND OT.local_ontology_id IN (");
		
		for (String localOntologyID : localOntologyIDs) {
			queryb.append("'");
			queryb.append(localOntologyID);
			queryb.append("', ");
		}
		queryb.delete(queryb.length()-2, queryb.length());
		queryb.append(");");

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
	
	//**********************Annotations Statistics ******************/
	  
	public boolean indexesExist(){
		boolean isIndexExist= false;
		try {			 			
			ResultSet rSet = this.executeSQLQuery("SHOW INDEX FROM "+ this.getTableSQLName());
			if(rSet.first()){
				isIndexExist= true;
			} 
			
			rSet.close();
		} 
		catch (SQLException e) {
			logger.error("** PROBLEM **  Problem in getting index from " + this.getTableSQLName()+ " .", e);
		}
		
		return isIndexExist;
	}
	
	public boolean createIndexes() {
		boolean result = false;
		try{
			this.executeSQLUpdate(this.getIndexCreationQuery());
			result = true;
			}
		catch(SQLException e){
			logger.error("** PROBLEM ** Cannot delete the temporary table.", e);
		}
		 return result; 
	}
	
	/********************************* ENTRY CLASS *****************************************************/

	/**
	 * This class is a representation for an annotation extracted from OBR_XX_DAT table.
	 * 
	 * @author Adrien Coulet
	 * @version OBR_v0.2		
	 * @created 15-Dec-2008
	 */
	public static class ExpandedAnnotation {

		private String  localElementID;
		private String  localConceptID;
		private String  contextName;
		private String  childConceptLocalID;
		private Integer level;
		private String  mappedConceptLocalID;
		private String  mappingType;
		private Float   score;
		
		public ExpandedAnnotation(String localElementID, String localConceptID,
				String contextName, String childConceptLocalID, Integer level, 
				String mappedConceptLocalID, String mappingType, Float score) {
			super();
			this.localElementID       = localElementID;
			this.localConceptID       = localConceptID;
			this.contextName          = contextName;
			this.childConceptLocalID  = childConceptLocalID;
			this.level                = level;
			this.mappedConceptLocalID = mappedConceptLocalID;
			this.mappingType          = mappingType;
			this.score                = score;
			
		}

		public String getLocalElementId() {
			return localElementID;
		}

		public String getLocalConceptID() {
			return localConceptID;
		}

		public String getContextName() {
			return contextName;
		}
		

		public String getChildConceptLocalID() {
			return childConceptLocalID;
		}

		public Integer getLevel() {
			return level;
		}

		public String getMappedConceptLocalID() {
			return mappedConceptLocalID;
		}

		public String getMappingType() {
			return mappingType;
		}
		
		public Float getScore() {
			return score;
		}
		
		public String toString(){
			StringBuffer sb = new StringBuffer();
			sb.append("ElementEntry: [");
			sb.append(this.localElementID);
			sb.append(", ");
			sb.append(this.localConceptID);
			sb.append(", ");
			sb.append(this.contextName);
			sb.append(", ");
			sb.append(this.childConceptLocalID);
			sb.append(", ");
			sb.append(this.level);
			sb.append(", ");
			sb.append(this.mappedConceptLocalID);
			sb.append(", ");
			sb.append(this.mappingType);
			sb.append(", ");			
			sb.append(this.score);		
			sb.append("]");
			return sb.toString();
		}
	}
	
	/**
	 * This class is a representation for a OBR_XX_EAT table entry.
	 * 
	 * @author Adrien Coulet
	 * @version OBR_v0.2		
	 * @created 12-Nov-2008
	 */
	static class ExpandedAnnotationEntry {

		private String localElementID;
		private String localConceptID;
		private String contextName;
		private String childConceptID;
		private Integer level;
		private String  mappedConceptID;
		private String  mappingType;
		private String  distantConceptID;
		private Integer distance;
		private Boolean indexingDone;
				
		public ExpandedAnnotationEntry(String localElementID,
				String localConceptID, String contextName,
				String childConceptID, Integer level, String mappedConceptID,
				String mappingType, String distantConceptID, Integer distance, Boolean indexingDone) {
			super();
			this.localElementID = localElementID;
			this.localConceptID = localConceptID;
			this.contextName = contextName;
			this.childConceptID = childConceptID;
			this.level = level;
			this.mappedConceptID = mappedConceptID;
			this.mappingType = mappingType;
			this.distantConceptID = distantConceptID;
			this.distance = distance;
			this.indexingDone = indexingDone;
		}

		public String getLocalElementId() {
			return localElementID;
		}

		public String getLocalConceptID() {
			return localConceptID;
		}

		public String getContextName() {
			return contextName;
		}

		public String getChildConceptID() {
			return childConceptID;
		}

		public Integer getLevel() {
			return level;
		}

		public String getMappedConceptID() {
			return mappedConceptID;
		}

		public String getMappingType() {
			return mappingType;
		}

		public String getDistantConceptID() {
			return distantConceptID;
		}

		public Integer getDistance() {
			return distance;
		}

		public Boolean getIndexingDone() {
			return indexingDone;
		}

		public String toString(){
			StringBuffer sb = new StringBuffer();
			sb.append("expandedAnnotationEntry: [");
			sb.append(this.localElementID);
			sb.append(", ");
			sb.append(this.localConceptID);
			sb.append(", ");
			sb.append(this.contextName);
			sb.append(", ");
			sb.append(this.childConceptID);
			sb.append(", ");
			sb.append(this.level);
			sb.append(", ");
			sb.append(this.mappedConceptID);
			sb.append(", ");
			sb.append(this.mappingType);
			sb.append(", ");
			sb.append(this.distantConceptID);
			sb.append(", ");
			sb.append(this.distance);
			sb.append(", ");
			sb.append(this.indexingDone);
			sb.append("]");
			return sb.toString();
		}
	}

}

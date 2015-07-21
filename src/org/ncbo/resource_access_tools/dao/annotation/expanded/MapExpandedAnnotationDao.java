package org.ncbo.resource_access_tools.dao.annotation.expanded;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;

import org.ncbo.resource_access_tools.common.beans.DictionaryBean;
import org.ncbo.resource_access_tools.dao.annotation.DirectAnnotationDao;
import org.ncbo.resource_access_tools.enumeration.WorkflowStatusEnum;
import org.ncbo.resource_access_tools.util.MessageUtils;

import com.mysql.jdbc.exceptions.MySQLNonTransientConnectionException;

/**
 * This class is a representation for the the OBR DB OBR_XX_EAT table. The table contains 
 * the following columns:
 * 
 * <ul>   
 * <li> `element_id` int(11) unsigned NOT NULL,
 * <li> `concept_id` int(11) unsigned NOT NULL,
 * <li> `context_id` smallint(5) unsigned NOT NULL,
 * <li> `position_from` int(11) unsigned DEFAULT NULL,
 * <li> `position_to` int(11) unsigned DEFAULT NULL,
 * <li> `mapped_concept_id` int(11) unsigned DEFAULT NULL,
 * <li> `mapping_type` tinyint(1) unsigned NOT NULL,
 * <li> `workflow_status` tinyint(1) unsigned NOT NULL DEFAULT '0', *  
 * </ul>
 *  
 * @author Adrien Coulet, Clement Jonquet, Kuladip Yadav
 * @version OBR_v0.2		
 * @created 13-Nov-2008
 *
 */
public class MapExpandedAnnotationDao extends AbstractExpandedAnnotationDao {

	private static final String TABLE_SUFFIX = MessageUtils.getMessage("obr.map.expanded.annotation.table.suffix");
	private PreparedStatement deleteEntriesFromOntologyStatement;
	
	/**
	 * Creates a new ExpandedAnnotationTable with a given resourceID.
	 * The suffix that will be added for AnnotationTable is "_EAT".
	 */
	public MapExpandedAnnotationDao(String resourceID) {
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
		//logger.info("creation of the table "+ this.getTableSQLName());
		return "CREATE TABLE " + getTableSQLName() +" (" +					 
					"element_id INT(11) UNSIGNED NOT NULL, " +
					"concept_id INT(11) UNSIGNED NOT NULL, " +			
					"context_id SMALLINT(5) UNSIGNED NOT NULL, " +	
					"position_from INT(11) UNSIGNED, " +
					"position_to INT(11) UNSIGNED, " +
					"mapped_concept_id INT(11) UNSIGNED, " +
					"mapping_type TINYINT(1) UNSIGNED NOT NULL, " +					
					"workflow_status TINYINT(1) UNSIGNED NOT NULL DEFAULT '0', " +
					"INDEX X_" + this.getTableSQLName() +"_element_id USING BTREE(element_id), " +	
					"INDEX X_" + this.getTableSQLName() +"_concept_id USING BTREE(concept_id), " +	
					"INDEX X_" + this.getTableSQLName() +"_context_id USING BTREE(context_id), " +	
					"INDEX X_" + this.getTableSQLName() +"_mapped_concept_id USING BTREE(mapped_concept_id), " +	
					"INDEX X_" + this.getTableSQLName() +"_mapping_type USING BTREE(mapping_type), " +	
					"INDEX X_" + this.getTableSQLName() +"_workflow_status USING BTREE(workflow_status) " +	
					")ENGINE=MyISAM DEFAULT CHARSET=latin1;";				 
	}
	
	@Override
	public String getIndexCreationQuery(){
		  return "ALTER TABLE " + this.getTableSQLName() +
		      // TODO: Commented indexes as per tracker item #2136 
//	  			" ADD INDEX IDX_"+ this.getTableSQLName() +"_element_id(element_id), " +
//	  			" ADD INDEX IDX_"+ this.getTableSQLName() +"_concept_id(concept_id), " +
	  		 	" ADD INDEX IDX_"+ this.getTableSQLName() +"_workflow_status(workflow_status) "; 
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
		this.deleteEntriesFromOntologyStatement.close();
	}
	
	/****************************************** FUNCTIONS ON THE TABLE ***************************/ 

	@Override
	protected void openAddEntryStatement(){		 
	} 
		 
	//********************************* SEMANTIC EXPANSION FUNCTIONS *****************************************************/
	 
	/**
	 * Populates the table with mapping annotations computed 
	 * with the annotations contained in the given table. 
	 * Only the annotations for which mappingDone=false are selected in the given table.
	 *  
	 * @param DirectAnnotationDao
	 * @return Returns the number of mapping annotations created in the corresponding _EAT.
	 */
	public long mappingExpansion(DirectAnnotationDao annotationDao){
		long nbAnnotation;	 
		// Query Used :
		// 		INSERT obr_tr_expanded_annotation(element_id, concept_id, context_id, mapped_concept_id, mapping_type, indexing_done)
		//			SELECT element_id, MAPT.mapped_concept_id, context_id, DAT.concept_id, mapping_type, false 
		//				FROM obr_gm_annotation AS DAT, obs_map AS MAPT 
		//				WHERE DAT.concept_id = MAPT.concept_id
		//					AND mapping_done = false ;		
		
		StringBuffer queryb = new StringBuffer();
		queryb.append("INSERT ");
		queryb.append(this.getTableSQLName());
		queryb.append(" (element_id, concept_id, context_id, mapped_concept_id, mapping_type, position_from, position_to, workflow_status) SELECT element_id, MAPT.mapped_concept_id, context_id, ");
		queryb.append("DAT.concept_id, mapping_type, DAT.position_from, DAT.position_to, ");
		queryb.append(WorkflowStatusEnum.INDEXING_NOT_DONE.getStatus());	
		queryb.append(" FROM ");	   
		queryb.append(annotationDao.getTableSQLName());
		queryb.append(" AS DAT, ");		
		queryb.append(mapDao.getMemoryTableSQLName()); // JOin with memory map table
		queryb.append(" AS MAPT WHERE DAT.concept_id = MAPT.concept_id AND DAT.workflow_status = ");
		queryb.append(WorkflowStatusEnum.IS_A_CLOSURE_DONE.getStatus());		 
		queryb.append("; ");
		
		StringBuffer updatingQueryb = new StringBuffer();
		updatingQueryb.append("UPDATE ");
		updatingQueryb.append(annotationDao.getTableSQLName());
		updatingQueryb.append(" SET workflow_status = ");
		updatingQueryb.append(WorkflowStatusEnum.MAPPING_DONE.getStatus());
		updatingQueryb.append(" WHERE workflow_status = ");
		updatingQueryb.append(WorkflowStatusEnum.IS_A_CLOSURE_DONE.getStatus());
		
		try{
			nbAnnotation = this.executeWithStoreProcedure(this.getTableSQLName(), queryb.toString(), false);
			this.executeSQLUpdate(updatingQueryb.toString());
		}
		catch(SQLException e){
			logger.error("** PROBLEM ** Cannot execute the mapping expansion query on table " + this.getTableSQLName() +". 0 returned", e);
			nbAnnotation = 0;
		}
		return nbAnnotation;
	}
	
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
		queryb.append(conceptDao.getMemoryTableSQLName());	
		queryb.append(" CT, ");
		queryb.append(ontologyDao.getMemoryTableSQLName());
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
		
		/*queryb.append("DELETE EAT FROM ");
		queryb.append(this.getTableSQLName());		
		queryb.append(" EAT, ");
		queryb.append(conceptDao.getMemoryTableSQLName( ));	
		queryb.append(" CT, ");
		queryb.append(ontologyDao.getMemoryTableSQLName());
		queryb.append(" OT ");
		queryb.append(" WHERE EAT.concept_id = CT.id AND CT.ontology_id = OT.id AND OT.local_ontology_id IN (");
		
		for (String localOntologyID : localOntologyIDs) {
			queryb.append("'");
			queryb.append(localOntologyID);
			queryb.append("', ");
		}
		queryb.delete(queryb.length()-2, queryb.length());
		queryb.append(");");*/
		
		
		queryb.append("DELETE EAT FROM ");
		queryb.append(this.getTableSQLName());		
		queryb.append(" EAT  ");
		queryb.append(" WHERE EAT.concept_id ");
		queryb.append("IN ( SELECT id FROM  ");
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
	
	//**********************Annotations Statistics ******************/
	 
	/**
	 * 
	 *  Get number of Mapping Annotations for each ontlogyID
	 * @param dictionary 
	 * @param withCompleteDictionary 
	 *  
	 *  @return HashMap<Integer, Integer>
	 */
	public HashMap<Integer, Long> getMappingAnnotationStatistics(boolean withCompleteDictionary, DictionaryBean dictionary){
		HashMap<Integer, Long> annotationStats = new HashMap<Integer, Long>();
		
		StringBuffer queryb = new StringBuffer(); 
		if(withCompleteDictionary){
			queryb.append("SELECT CT.ontology_id, COUNT(EAT.concept_id) AS COUNT FROM ");
			queryb.append(this.getTableSQLName());		 	 
			queryb.append(" AS EAT, ");
			queryb.append(conceptDao.getMemoryTableSQLName());
			queryb.append(" AS CT WHERE EAT.concept_id=CT.id GROUP BY CT.ontology_id; ");		 
		}else{
			queryb.append("SELECT OT.id, COUNT(EAT.concept_id) AS COUNT FROM ");
			queryb.append(this.getTableSQLName());		 	 
			queryb.append(" AS EAT, ");
			queryb.append(conceptDao.getMemoryTableSQLName());
			queryb.append(" AS CT, ");
			queryb.append(ontologyDao.getMemoryTableSQLName());
			queryb.append(" AS OT WHERE EAT.concept_id=CT.id AND CT.ontology_id=OT.id AND OT.dictionary_id = ");
			queryb.append(dictionary.getDictionaryId());				 
			queryb.append( " GROUP BY OT.id; ");
		}
		
		try {			 			
			ResultSet rSet = this.executeSQLQuery(queryb.toString());
			while(rSet.next()){
				annotationStats.put(rSet.getInt(1), rSet.getLong(2));
			}			
			rSet.close();
		}
		catch (MySQLNonTransientConnectionException e) {			 
			return this.getMappingAnnotationStatistics(withCompleteDictionary, dictionary);
		}
		catch (SQLException e) {
			logger.error("** PROBLEM ** Cannot get mapping annotations statistics from " + this.getTableSQLName()+ " .", e);
		}
		return annotationStats;
		 
	} 
}

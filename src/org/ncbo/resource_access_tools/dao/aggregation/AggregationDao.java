package org.ncbo.resource_access_tools.dao.aggregation;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;

import obs.common.utils.ExecutionTimer;

import org.ncbo.resource_access_tools.dao.AbstractObrDao;
import org.ncbo.resource_access_tools.dao.annotation.DirectAnnotationDao;
import org.ncbo.resource_access_tools.dao.annotation.expanded.IsaExpandedAnnotationDao;
import org.ncbo.resource_access_tools.dao.annotation.expanded.MapExpandedAnnotationDao;
import org.ncbo.resource_access_tools.dao.element.ElementDao;
import org.ncbo.resource_access_tools.enumeration.ResourceType;
import org.ncbo.resource_access_tools.enumeration.WorkflowStatusEnum;
import org.ncbo.resource_access_tools.populate.ObrWeight;
import org.ncbo.resource_access_tools.util.MessageUtils;

import com.mysql.jdbc.exceptions.MySQLIntegrityConstraintViolationException;
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
public class AggregationDao extends AbstractObrDao {

	private static final String TABLE_SUFFIX = MessageUtils.getMessage("obr.aggregation.table.suffix"); // element Index Table
	private static final String WORKFLOW_TABLE_SUFFIX = MessageUtils.getMessage("obr.aggregation.workflow.table.suffix");

	private PreparedStatement addEntryStatement;
	private PreparedStatement deleteEntriesFromOntologyStatement;

	/**
	 * Creates a new AggregationDao with a given resourceID.
	 * The suffix that will be added for Aggregation table is "_aggregation".
	 */
	public AggregationDao(String resourceID) {
		super(resourceID, WORKFLOW_TABLE_SUFFIX);
	}

	/**
	 * Returns the SQL table name for a given resourceID
	 */
	public static String name(String resourceID){
		return OBR_PREFIX + resourceID.toLowerCase() + WORKFLOW_TABLE_SUFFIX;
	}

	private String getSortedTableSQLName(){
		return OBR_PREFIX + this.resourceID.toLowerCase() + TABLE_SUFFIX;
	}

	@Override
	protected String creationQuery(){
		return "CREATE TABLE " + getTableSQLName() +" (" +
					"element_id INT UNSIGNED NOT NULL, " +
					"concept_id INT UNSIGNED NOT NULL, " +
					"score FLOAT, " +
					"UNIQUE INDEX X_" + getTableSQLName() +"_element_id USING BTREE(element_id, concept_id) " +
					//"INDEX X_" + getTableSQLName() +"_concept_id(concept_id) USING BTREE " +
				")ENGINE=MyISAM DEFAULT CHARSET=latin1; ;";
	}

	protected String tempTableCreationQuery(){
		return "CREATE TABLE " + getTempTableSQLName() +" (" +
					"element_id INT UNSIGNED NOT NULL, " +
					"concept_id INT UNSIGNED NOT NULL, " +
					"score FLOAT" +
				")ENGINE=MyISAM DEFAULT CHARSET=latin1; ;";
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

	/**
	 * Add an new entry in corresponding SQL table.
	 * @return True if the entry was added to the SQL table, false if a problem occurred during insertion.
	 */
	public boolean addEntry(AggregationAnnotation entry){
		boolean inserted = false;
		try {
			this.addEntryStatement.setString(1, entry.getLocalElementId());
			this.addEntryStatement.setString(2, entry.getLocalConceptID());
			this.addEntryStatement.setFloat(3, entry.getScore());
			this.executeSQLUpdate(this.addEntryStatement);
			inserted = true;
		}
		catch (MySQLNonTransientConnectionException e) {
			this.openAddEntryStatement();
			return this.addEntry(entry);
		}
		catch (MySQLIntegrityConstraintViolationException e){
			//logger.info("Table " + this.getTableSQLName() + " already contains an entry for the concept: " + entry.getLocalConceptID() +".");
		}
		catch (SQLException e) {
			logger.error("** PROBLEM ** Cannot add an entry on table " + this.getTableSQLName(), e);
			logger.error(entry.toString());
		}
		return inserted;
	}

	// ********************************* AGGREGATION FUNCTIONS  *****************************************************/

	/**
	 * Index the content of _DAT and _EAT in the table by computing the right score.
	 * Returns the number of annotations added to the table.
	 */
	public long aggregation(ObrWeight weights){
		ExecutionTimer timer = new ExecutionTimer();
		// Load obr_context table in memeory
		contextTableDao.loadTableIntoMemory(this.resourceID);

		long nbAnnotation = 0;
		// Adds the direct annotations done to aggregation table
		String directAnnotationAggregationQuery = aggregationQueryForDirectAnnotations(weights);
		timer.start();
		try{
			nbAnnotation = this.executeSQLUpdate(directAnnotationAggregationQuery);
			}
		catch(SQLException e){
			logger.error("** PROBLEM ** Cannot aggregate annotations from annotation table ", e);
		}
		timer.end();
		logger.info("\t" + nbAnnotation + " annotations aggregated with direct annotations in : " + timer.millisecondsToTimeString(timer.duration()));

		// Changing the workflow_status flags on DAT
		StringBuffer updatingQueryb1 = new StringBuffer();
		updatingQueryb1.append("UPDATE ");
		updatingQueryb1.append(DirectAnnotationDao.name(this.resourceID));
		updatingQueryb1.append(" SET workflow_status = ");
		updatingQueryb1.append(WorkflowStatusEnum.INDEXING_DONE.getStatus());
		updatingQueryb1.append(" WHERE workflow_status = ");
		updatingQueryb1.append(WorkflowStatusEnum.MAPPING_DONE.getStatus());

		timer.reset();
		timer.start();
		try{
			nbAnnotation = this.executeSQLUpdate(updatingQueryb1.toString());
			}
		catch(SQLException e){
			logger.error("** PROBLEM ** Cannot switch workflow_status flags on DAT.", e);
		}
		timer.end();
		logger.info("\tWorkflow_status updated to "+ WorkflowStatusEnum.INDEXING_DONE.getStatus()
				+ " in table " + DirectAnnotationDao.name(this.resourceID)
				+ " in :" + timer.millisecondsToTimeString(timer.duration()));

		// Adds to _aggregation the isa expanded annotations.
		String isaExpansionAggregationQuery = aggregationQueryForIsaExpandedAnnotations(weights);
		timer.reset();
		timer.start();
		try{
			nbAnnotation = this.executeSQLUpdate(isaExpansionAggregationQuery);
			}
		catch(SQLException e){
			logger.error("** PROBLEM ** Cannot index isa expanded annotations from _EAT.", e);
		}
		timer.end();
		logger.info("\t" +nbAnnotation + " annotations aggregated with isa expanded annotations in : " + timer.millisecondsToTimeString(timer.duration()) );

		// Switches the indexingDone flags on EAT
		StringBuffer updatingQueryb2 = new StringBuffer();
		updatingQueryb2.append("UPDATE ");
		updatingQueryb2.append(IsaExpandedAnnotationDao.name(this.resourceID));
		updatingQueryb2.append(" SET workflow_status = ");
		updatingQueryb2.append(WorkflowStatusEnum.INDEXING_DONE.getStatus());
		updatingQueryb2.append(" WHERE workflow_status = ");
		updatingQueryb2.append(WorkflowStatusEnum.INDEXING_NOT_DONE.getStatus());
		timer.reset();
		timer.start();
		try{
			 nbAnnotation = this.executeSQLUpdate(updatingQueryb2.toString());
			 }
		catch(SQLException e){
			logger.error("** PROBLEM ** Cannot switch indexingDone flags on _EAT.", e);
		}
		timer.end();
		logger.info("\tworkflow_status updated to "+ WorkflowStatusEnum.INDEXING_DONE.getStatus()+ " in table "
				+ IsaExpandedAnnotationDao.name(this.resourceID)
				+ " in : " + timer.millisecondsToTimeString(timer.duration()));


		// Adds to _aggregation the mapping expanded annotations.
		String mappingExpansionAggregationQuery = aggregationQueryForMapExpandedAnnotations(weights);
		timer.reset();
		timer.start();
		try{
			nbAnnotation = this.executeSQLUpdate(mappingExpansionAggregationQuery);
			}
		catch(SQLException e){
			logger.error("** PROBLEM ** Cannot index mapping expanded annotations from _EAT.", e);
		}
		timer.end();
		logger.info("\t" + nbAnnotation + " annotations aggregated with mapping expanded annotations in : "
				+ timer.millisecondsToTimeString(timer.duration()));

		// Switches the workflow_status flags on mapping annotation
		StringBuffer updatingQueryb3 = new StringBuffer();
		updatingQueryb3.append("UPDATE ");
		updatingQueryb3.append(MapExpandedAnnotationDao.name(this.resourceID));
		updatingQueryb3.append(" SET workflow_status = ");
		updatingQueryb3.append(WorkflowStatusEnum.INDEXING_DONE.getStatus());
		updatingQueryb3.append(" WHERE workflow_status = ");
		updatingQueryb3.append(WorkflowStatusEnum.INDEXING_NOT_DONE.getStatus());
		timer.reset();
		timer.start();
		try{
			 nbAnnotation = this.executeSQLUpdate(updatingQueryb3.toString());
		  }
		catch(SQLException e){
			logger.error("** PROBLEM ** Cannot switch workflow_status flags on " + MapExpandedAnnotationDao.name(this.resourceID), e);
		}
		timer.end();
		logger.info("\tworkflow_status updated to "+ WorkflowStatusEnum.INDEXING_DONE.getStatus()+ " in table "
				+ MapExpandedAnnotationDao.name(this.resourceID)
				+ " in  : " + timer.millisecondsToTimeString(timer.duration()));
		return this.numberOfEntry();
	}

	/**
	 *
	 *
	 * @param resourceType
	 */
	public boolean sortAggregation(ResourceType resourceType){
		try{
			if(ResourceType.BIG== resourceType){
				this.callStoredProcedure(this.getTableSQLName(), this.getSortedTableSQLName(), "1");
			}else{
				this.callStoredProcedure(this.getTableSQLName(), this.getSortedTableSQLName(), "0");
			}
			return true;
		}
		catch(Exception e){
			logger.error("** PROBLEM ** Cannot sort aggregation table " + this.getTableSQLName(), e);
		}
		return false;
	}

	/**
	 *
	 *
	 * @param weights
	 * @return
	 */
	private String aggregationQueryForDirectAnnotations(ObrWeight weights){

		StringBuffer query = new StringBuffer();
		query.append("INSERT INTO ");
		query.append(this.getTableSQLName());
		query.append(" (element_id, concept_id, score) SELECT element_id, DAT.concept_id, ");
		query.append("IF(DAT.term_id IS NULL, @s:=(");
		query.append(weights.getReportedDA());
		query.append("*weight), ");
		query.append("IF(TT.is_preferred,@s:=(");
		query.append(weights.getPreferredNameDA());
		query.append("*weight),@s:=(");
		query.append(weights.getSynonymDA());
		query.append("*weight))");
		query.append(") calc_score FROM ");
		query.append(DirectAnnotationDao.name(this.resourceID));
		query.append(" DAT LEFT JOIN ");
		query.append(termDao.getMemoryTableSQLName());
		query.append(" TT ON DAT.term_id= TT.id, ");
		query.append(contextTableDao.getMemoryTableSQLName());
		query.append(" CXT WHERE DAT.context_id = CXT.id AND workflow_status= ");
		query.append(WorkflowStatusEnum.MAPPING_DONE.getStatus());
		query.append(" ON DUPLICATE KEY UPDATE score=score+@s ;");
		return query.toString();
	}

	private String aggregationQueryForIsaExpandedAnnotations(ObrWeight weights){
		StringBuffer query = new StringBuffer();
		query.append("INSERT INTO ");
		query.append(this.getTableSQLName());
		query.append(" (element_id, concept_id, score) SELECT element_id, EAT.concept_id, @s:=(");
		query.append("FLOOR(10*EXP(-").append(weights.getIsaFactor());
		query.append("* EAT.parent_level)+1)");
		query.append("*weight) FROM ");
		query.append(IsaExpandedAnnotationDao.name(this.resourceID));
		query.append(" EAT, ");
		query.append(contextTableDao.getMemoryTableSQLName());
		query.append(" CXT WHERE EAT.context_id= CXT.id AND workflow_status=");
		query.append(WorkflowStatusEnum.INDEXING_NOT_DONE.getStatus());
		query.append(" ON DUPLICATE KEY UPDATE score=score+@s;");

		return query.toString();
	}

	private String aggregationQueryForMapExpandedAnnotations(ObrWeight weights){
		StringBuffer query = new StringBuffer();
		query.append("INSERT INTO ");
		query.append(this.getTableSQLName());
		query.append(" (element_id, concept_id, score) SELECT element_id, EAT.concept_id, @s:=(");
		query.append(weights.getMappingEA());
		query.append("*weight) FROM ");
		query.append(MapExpandedAnnotationDao.name(this.resourceID));
		query.append(" EAT, ");
		query.append(contextTableDao.getMemoryTableSQLName());
		query.append(" CXT WHERE EAT.context_id= CXT.id AND workflow_status=");
		query.append(WorkflowStatusEnum.INDEXING_NOT_DONE.getStatus());
		query.append(" ON DUPLICATE KEY UPDATE score=score+@s;");
		return query.toString();
	}

	//********************************* DELETE FUNCTIONS *****************************************************/

	private void openDeleteEntriesFromOntologyStatement(){
		// Query Used :
		//	DELETE IT FROM obr_tr_index IT, obs_concept CT, obs_ontology OT
		//		WHERE IT.conept_id = CT.id
		//			AND CT.ontology_id = OT.id
		//			AND OT.local_ontology_id = ?;
		StringBuffer queryb = new StringBuffer();
		queryb.append("DELETE IT FROM ");
		queryb.append(this.getTableSQLName());
		queryb.append(" IT, ");
		queryb.append(conceptDao.getMemoryTableSQLName( ));
		queryb.append(" CT, ");
		queryb.append(ontologyDao.getMemoryTableSQLName());
		queryb.append(" OT ");
		queryb.append(" WHERE IT.concept_id = CT.id AND CT.ontology_id = OT.id AND OT.local_ontology_id = ?");

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
	 * Deletes the rows for indexing done with a concept in the given list of localOntologyIDs.
	 *
	 * @param {@code List} of local ontology ids
	 * @return True if the rows were successfully removed.
	 */
	public boolean deleteEntriesFromOntologies(List<String> localOntologyIDs){
		boolean deleted = false;
		StringBuffer queryb = new StringBuffer();
		/*queryb.append("DELETE IT FROM ");
		queryb.append(this.getTableSQLName());
		queryb.append(" IT, ");
		queryb.append(conceptDao.getMemoryTableSQLName());
		queryb.append(" CT, ");
		queryb.append(ontologyDao.getMemoryTableSQLName());
		queryb.append(" OT ");
		queryb.append(" WHERE IT.concept_id = CT.id AND CT.ontology_id = OT.id AND OT.local_ontology_id IN (");

		for (String localOntologyID : localOntologyIDs) {
			queryb.append("'");
			queryb.append(localOntologyID);
			queryb.append("', ");
		}
		queryb.delete(queryb.length()-2, queryb.length());
		queryb.append(");");*/

		queryb.append("DELETE IT FROM ");
		queryb.append(this.getTableSQLName());
		queryb.append(" IT ");
		queryb.append(" WHERE IT.concept_id IN ");
		queryb.append(" ( SELECT id FROM  ");
		queryb.append(conceptDao.getMemoryTableSQLName());
		queryb.append(" CT ");
		queryb.append(" WHERE CT.ontology_id IN ");
		queryb.append(" ( SELECT id FROM  ");
		queryb.append(ontologyDao.getMemoryTableSQLName());
		queryb.append(" OT ");

		queryb.append(" Where OT.local_ontology_id IN ( ");
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

	//**********************************Statistics Method****************

	/**
	 * This method gives number of aggregated annotations for each ontologyID
	 * @param dictionary
	 * @param withCompleteDictionary
	 *
	 * @return map containing number of aggregated annotations for each ontologyID as key.
	 */
	public HashMap<Integer, Long> getAggregatedAnnotationStatistics(boolean withCompleteDictionary, DictionaryBean dictionary){
		HashMap<Integer, Long> annotationStats = new HashMap<Integer, Long>();

		StringBuffer queryb = new StringBuffer();
		if(withCompleteDictionary){
			queryb.append("SELECT CT.ontology_id, COUNT(IT.concept_id) AS COUNT FROM ");
			queryb.append(this.getTableSQLName());
			queryb.append(" AS IT, ");
			queryb.append(conceptDao.getMemoryTableSQLName());
			queryb.append(" AS CT WHERE IT.concept_id=CT.id GROUP BY CT.ontology_id; ");
		}else{
			queryb.append("SELECT OT.id, COUNT(IT.concept_id) AS COUNT FROM ");
			queryb.append(this.getTableSQLName());
			queryb.append(" AS IT, ");
			queryb.append(conceptDao.getMemoryTableSQLName());
			queryb.append(" AS CT, ");
			queryb.append(ontologyDao.getMemoryTableSQLName());
			queryb.append(" AS OT WHERE IT.concept_id=CT.id AND CT.ontology_id=OT.id AND OT.dictionary_id = ");
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
			return this.getAggregatedAnnotationStatistics(withCompleteDictionary, dictionary);
		}
		catch (SQLException e) {
			logger.error("** PROBLEM ** Cannot get aggregated annotations statistics from "+this.getTableSQLName()+" .", e);
		}
		return annotationStats;

	}

	// ********************************* ENTRY CLASS *****************************************************/

	/**
	 * This class is a representation for an annotation in OBR_XX_IT table.
	 *
	 * @author Adrien Coulet, Clement Jonquet
	 * @version OBR_v0.2
	 * @created 13-Nov-2008
	 */
	static class AggregationAnnotation {

		private String localElementID;
		private String localConceptID;
		private float score;

		public AggregationAnnotation(String localElementID, String localConceptID, float score) {
			super();
			this.localElementID = localElementID;
			this.localConceptID = localConceptID;
			this.score = score;
		}

		public String getLocalElementId() {
			return localElementID;
		}

		public String getLocalConceptID() {
			return localConceptID;
		}

		public float getScore() {
			return score;
		}

		public String toString(){
			StringBuffer sb = new StringBuffer();
			sb.append("ElementEntry: [");
			sb.append(this.localElementID);
			sb.append(", ");
			sb.append(this.localConceptID);
			sb.append(", ");
			sb.append(this.score);
			sb.append("]");
			return sb.toString();
		}
	}

	/**
	 * This class is a representation for a obr_xx_aggregation table entry.
	 *
	 * @author Adrien Coulet, Clement Jonquet
	 * @version OBR_v0.2
	 * @created 12-Nov-2008

	 */
	public static class AggregationEntry {

		private Integer elementID;
		private Integer conceptID;
		private float score;

		public AggregationEntry(Integer elementID, Integer conceptID, float score) {
			super();
			this.elementID = elementID;
			this.conceptID = conceptID;
			this.score = score;
		}

		public Integer getElementID() {
			return elementID;
		}

		public Integer getConceptID() {
			return conceptID;
		}

		public float getScore() {
			return score;
		}

		public String toString(){
			StringBuffer sb = new StringBuffer();
			sb.append("AggregationEntry: [");
			sb.append(this.elementID);
			sb.append(", ");
			sb.append(this.conceptID);
			sb.append(", ");
			sb.append(this.score);
			sb.append("]");
			return sb.toString();
		}
	}
}

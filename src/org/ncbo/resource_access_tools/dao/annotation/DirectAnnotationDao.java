package org.ncbo.resource_access_tools.dao.annotation;

import java.io.File;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import obs.common.utils.ExecutionTimer;

import org.ncbo.resource_access_tools.dao.AbstractObrDao;
import org.ncbo.resource_access_tools.dao.element.ElementDao;
import org.ncbo.resource_access_tools.enumeration.WorkflowStatusEnum;
import org.ncbo.resource_access_tools.util.FileResourceParameters;
import org.ncbo.resource_access_tools.util.MessageUtils;

import com.mysql.jdbc.exceptions.MySQLIntegrityConstraintViolationException;
import com.mysql.jdbc.exceptions.MySQLNonTransientConnectionException;

/**
 * This class is a representation for the the OBR DB obr_xx_annotation table. The table contains
 * the following columns:
 *
 * <ul>
 * <li>  `element_id` int(11) unsigned NOT NULL,
 * <li>  `concept_id` int(11) unsigned NOT NULL,
 * <li>  `context_id` smallint(5) unsigned NOT NULL,
 * <li>  `position_from` int(11) DEFAULT NULL,
 * <li>  `position_to` int(11) DEFAULT NULL,
 * <li>  `term_id` int(11) unsigned DEFAULT NULL,
 * <li>  `dictionary_id` smallint(5) unsigned NOT NULL,
 * <li>  `workflow_status` tinyint(1) unsigned NOT NULL DEFAULT '0',
 * </ul>
 *
 * @author Adrien Coulet, Clement Jonquet
 * @version OBR_v0.2
 * @created 12-Nov-2008
 *
 */
public class DirectAnnotationDao extends AbstractObrDao {

	private static final String TABLE_SUFFIX = MessageUtils.getMessage("obr.annotation.table.suffix");

	private PreparedStatement addEntryStatement;
	private PreparedStatement addMgrepEntryStatement;
	private PreparedStatement deleteEntriesFromOntologyStatement;

	/**
	 * Creates a new DirectAnnotationTable with a given resourceID.
	 * The suffix that will be added for AnnotationTable is "_DAT".
	 */
	public DirectAnnotationDao(String resourceID) {
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
		return "CREATE TABLE " + this.getTableSQLName() +" (" +
					"element_id INT(11) UNSIGNED NOT NULL, " +
					"concept_id INT(11) UNSIGNED NOT NULL, " +
					"context_id SMALLINT(5) UNSIGNED NOT NULL, " +
					"position_from INT(11), " +
					"position_to INT(11), " +
					"term_id INT(11) UNSIGNED, " +
					"dictionary_id SMALLINT(5) UNSIGNED NOT NULL, " +
					"workflow_status TINYINT(1) UNSIGNED NOT NULL DEFAULT '0', " +
					"INDEX X_" + this.getTableSQLName() +"_element_id  USING BTREE(element_id), " +	//no rajesh modification
					"INDEX X_" + this.getTableSQLName() +"_concept_id USING BTREE(concept_id), " +
					"INDEX X_" + this.getTableSQLName() +"_context_id USING BTREE(context_id), " +
					"INDEX X_" + this.getTableSQLName() +"_term_id USING BTREE(term_id), " +
					"INDEX X_" + this.getTableSQLName() +"_dictionary_id USING BTREE(dictionary_id), " +
					"INDEX X_" + this.getTableSQLName() +"_workflow_status USING BTREE(workflow_status) " +
				")ENGINE=MyISAM DEFAULT CHARSET=latin1;";
	}

	protected String getIndexCreationQuery(){
		  return "ALTER TABLE " + this.getTableSQLName() +
	      // TODO: Commented indexes as per tracker item #2136
//	  			" ADD INDEX IDX_"+ this.getTableSQLName() +"_element_id(element_id), " +
//	  			" ADD INDEX IDX_"+ this.getTableSQLName() +"_concept_id(concept_id), " +
//	  			" ADD INDEX IDX_"+ this.getTableSQLName() +"_dictionary_id(dictionary_id), " +
	  			" ADD INDEX IDX_"+ this.getTableSQLName() +"_workflow_status(workflow_status) ";
	}

	@Override
	protected void openPreparedStatements() {
		super.openPreparedStatements();
		this.openAddEntryStatement();
		this.openAddMgrepEntryStatement();
		this.openDeleteEntriesFromOntologyStatement();
	}

	@Override
	protected void closePreparedStatements() throws SQLException {
		super.closePreparedStatements();
		this.addEntryStatement.close();
		this.addMgrepEntryStatement.close();
		this.deleteEntriesFromOntologyStatement.close();
	}

	/****************************************** FUNCTIONS ON THE TABLE ***************************/

	@Override
	protected void openAddEntryStatement(){
		StringBuffer queryb = new StringBuffer();
		queryb.append("INSERT INTO ");
		queryb.append(this.getTableSQLName());
		queryb.append(" (element_id, concept_id, context_id, dictionary_id, workflow_status) ");
		queryb.append("VALUES (");
			// sub query to get the elementID from the localElementID
			queryb.append("(SELECT id FROM ");
			queryb.append(ElementDao.name(this.resourceID));
			queryb.append(" WHERE local_element_id=?)");
		queryb.append("	,");
			// sub query to get the conceptID from the localConceptID
			queryb.append("(SELECT id FROM ");
			queryb.append(conceptDao.getTableSQLName());
			queryb.append(" WHERE local_concept_id=?)");
		queryb.append("	,");
			// sub query to get the contextID from the contextName
			queryb.append("(SELECT id FROM ");
			queryb.append(contextTableDao.getTableSQLName());
			queryb.append(" WHERE name=?)");
		queryb.append(",?,?)");
		this.addEntryStatement = this.prepareSQLStatement(queryb.toString());
	}

	/**
	 * Add an new entry in corresponding SQL table.
	 * @return True if the entry was added to the SQL table, false if a problem occurred during insertion.
	 */
	public boolean addEntry(DirectAnnotationEntry entry ){
		boolean inserted = false;
		try {
			this.addEntryStatement.setString (1, entry.getLocalElementId());
			this.addEntryStatement.setString (2, entry.getLocalConceptID());
			this.addEntryStatement.setString (3, entry.getContextName());
			this.addEntryStatement.setInt    (4, entry.getDictionaryId());
			this.addEntryStatement.setInt(5, entry.getWorkflowStatus());
			this.executeSQLUpdate(this.addEntryStatement);
			inserted = true;
		}
		catch (MySQLNonTransientConnectionException e) {
			this.openAddEntryStatement();
			return this.addEntry(entry );
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

	private void openAddMgrepEntryStatement(){
		StringBuffer queryb = new StringBuffer();
		queryb.append("INSERT INTO ");
		queryb.append(this.getTableSQLName());
		queryb.append(" (element_id, concept_id, context_id, term_id, "+this.getTableSQLName()+".position_from, "+this.getTableSQLName()+".position_to, dictionary_id, workflow_status) ");
		queryb.append("VALUES (");
			// sub query to get the elementID from the localElementID
			queryb.append("(SELECT id FROM ");
			queryb.append(ElementDao.name(this.resourceID));
			queryb.append(" WHERE local_element_id=?)");
		queryb.append("	,");
			// sub query to get the conceptID from the localConceptID
			queryb.append("(SELECT id FROM ");
			queryb.append(conceptDao.getTableSQLName());
			queryb.append(" WHERE local_concept_id=?)");
		queryb.append("	,");
			// sub query to get the contextID from the contextName
			queryb.append("(SELECT id FROM ");
			queryb.append(contextTableDao.getTableSQLName());
			queryb.append(" WHERE name=?)");
		queryb.append(",?,?,?,?,?)");
		this.addMgrepEntryStatement = this.prepareSQLStatement(queryb.toString());
	}

	/**
	 * Add an new entry in corresponding SQL table.
	 * This entry corresponds to an annotation done with Mgrep.
	 * @return True if the entry was added to the SQL table, false if a problem occurred during insertion.
	 */
	public boolean addMgrepEntry(DirectMgrepAnnotationEntry entry){
		boolean inserted = false;
		try {
			this.addMgrepEntryStatement.setString (1, entry.getLocalElementId());
			this.addMgrepEntryStatement.setString (2, entry.getLocalConceptID());
			this.addMgrepEntryStatement.setString (3, entry.getContextName());
			this.addMgrepEntryStatement.setInt    (4, entry.getTermID());
			this.addMgrepEntryStatement.setInt    (5, entry.getFrom());
			this.addMgrepEntryStatement.setInt    (6, entry.getTo());
			this.addMgrepEntryStatement.setInt    (7, entry.getDictionaryId());
			this.addMgrepEntryStatement.setInt	  (8, entry.getWorkflowStatus());

			this.executeSQLUpdate(this.addMgrepEntryStatement);
			inserted = true;
		}
		catch (MySQLNonTransientConnectionException e) {
			this.openAddMgrepEntryStatement();
			return this.addMgrepEntry(entry);
		}
		catch (MySQLIntegrityConstraintViolationException e){
			//logger.error("Table " + this.getTableSQLName() + " already contains an entry for the concept: " + entry.getLocalConceptID() +".");
		}
		catch (SQLException e) {
			logger.error("** PROBLEM ** Cannot add an Mgrep entry "+entry.toString()+" on table " + this.getTableSQLName(), e);
			logger.error(entry.toString());
		}
		return inserted;
	}

	/**
	 * Add a set of DirectAnnotationEntry to the table.
	 * @param HashSet<DirectAnnotationEntry> entries
	 * @return the number of added entries
	 */
	public int addEntries(HashSet<DirectAnnotationEntry> entries){
		int nbInserted = 0;

		for(DirectAnnotationEntry entry: entries){
			try {
				addEntry(entry);
				 nbInserted++;
			}catch (Exception e) {
				logger.error("** PROBLEM ** Cannot add " + entry.toString() + "on table " + this.getTableSQLName());

			}
		}

		return nbInserted;
	}

	//********************************* MGREP FUNCTIONS *****************************************************

	/**
	 * Loads a Mgrep file (that respects the Mgrep specification, 5 columns: termID/from/to/elementID/contextID) into the table and completes
	 * the information in the table.
	 * Returns the number of annotations added to the table.
	 */
	public long loadMgrepFile(File mgrepFile, int dictionaryID){
		long nbAnnotation;
		ExecutionTimer timer = new ExecutionTimer();

		// Creates a temporary table with the same columns than the Mgrep result file
		/* CREATE TEMPORARY TABLE OBR_TR_MGREP
    	(termID INT UNSIGNED, OBR_TR_MGREP.from INT UNSIGNED, OBR_TR_MGREP.to INT UNSIGNED, elementID INT UNSIGNED, contextID INT UNSIGNED,); */

		StringBuffer createQuery = new StringBuffer();
		createQuery.append("CREATE TABLE ");
		createQuery.append(this.getTableSQLName());
		createQuery.append("_MGREP (term_id INT UNSIGNED, ");
		createQuery.append(this.getTableSQLName());
		createQuery.append("_MGREP.position_from INT UNSIGNED, ");
		createQuery.append(this.getTableSQLName());
		createQuery.append("_MGREP.position_to INT UNSIGNED, element_id INT UNSIGNED, context_id INT UNSIGNED);");
		try{
			this.executeSQLUpdate(createQuery.toString());
			}
		catch(SQLException e){
			logger.error("** PROBLEM ** Cannot create temporary table to load the file " + mgrepFile.getName(), e);
		}

		// Loads the Mgrep results from the file to the temporary table
		/* Example of query
		 * LOAD DATA INFILE '/ncbodata/OBR/MgrepResult/OBR_RESOURCE_TR_V1_MGREP.txt.mgrep'
			INTO TABLE OBR_TR_MGREP FIELDS TERMINATED BY '	' (termID, OBR_TR_MGREP.from, OBR_TR_MGREP.to, elementID, contextID) ; */
		// DO NOT USE a embedded SELECT IT SLOWS DOWN SIGNIFICANTLY THE QUERY
		StringBuffer loadingQuery = new StringBuffer();
		loadingQuery.append("LOAD DATA LOCAL INFILE '");
		loadingQuery.append(FileResourceParameters.mgrepOutputFolder());
		loadingQuery.append(mgrepFile.getName());
		loadingQuery.append("' INTO TABLE ");
		loadingQuery.append(this.getTableSQLName());
		loadingQuery.append("_MGREP FIELDS TERMINATED BY '\t' (term_id, ");
		loadingQuery.append(this.getTableSQLName());
		loadingQuery.append("_MGREP.position_from, ");
		loadingQuery.append(this.getTableSQLName());
		loadingQuery.append("_MGREP.position_to, element_id, context_id);");
		timer.start();
		try{
			this.executeSQLUpdate(loadingQuery.toString());
			}
		catch(SQLException e){
			logger.error("** PROBLEM ** Cannot load the file " + mgrepFile.getName(), e);
		}

		timer.end();
		logger.info("MGREP Table created in:"
				+ timer.millisecondsToTimeString(timer.duration()) + "\n");

		// Joins the temporary table and OBS_TT to populate the table
		/* INSERT INTO OBR_TR_DAT (elementID, conceptID, contextID, termID, OBR_TR_DAT.from, OBR_TR_DAT.to, dictionaryID, isaClosureDone, mappingDone, distanceDone, indexingDone)
 			SELECT elementID, conceptID, contextID, OBR_TR_MGREP.termID, OBR_TR_MGREP.from, OBR_TR_MGREP.to, 1, false, false, false, false
    		FROM OBR_TR_MGREP, OBS_TT WHERE OBR_TR_MGREP.termID=OBS_TT.termID;  */

		StringBuffer joinQuery = new StringBuffer();
		joinQuery.append("INSERT INTO ");
		joinQuery.append(this.getTableSQLName());
		joinQuery.append(" (element_id, concept_id, context_id, term_id, ");
		joinQuery.append(this.getTableSQLName());
		joinQuery.append(".position_from, ");
		joinQuery.append(this.getTableSQLName());
		joinQuery.append(".position_to, dictionary_id, workflow_status) SELECT element_id, concept_id, context_id, ");
		joinQuery.append(this.getTableSQLName());
		joinQuery.append("_MGREP.term_id, ");
		joinQuery.append(this.getTableSQLName());
		joinQuery.append("_MGREP.position_from, ");
		joinQuery.append(this.getTableSQLName());
		joinQuery.append("_MGREP.position_to, ");
		joinQuery.append(dictionaryID);
		joinQuery.append(", ");
		joinQuery.append(WorkflowStatusEnum.DIRECT_ANNOTATION_DONE.getStatus());
		joinQuery.append(" FROM ");
		joinQuery.append(this.getTableSQLName());
		joinQuery.append("_MGREP, ");
		joinQuery.append(termDao.getMemoryTableSQLName());
		joinQuery.append(" TT WHERE ");
		joinQuery.append(this.getTableSQLName());
		joinQuery.append("_MGREP.term_id= TT.id ;");
		timer.reset();
		timer.start();
		try{
			nbAnnotation = this.executeWithStoreProcedure(this.getTableSQLName(), joinQuery.toString(), false);
			}
		catch(SQLException e){
			logger.error("** PROBLEM ** Cannot join the temporary table and OBS_TT to load the file " + mgrepFile.getName()+". 0 returned", e);
			nbAnnotation = 0;
		}
		timer.end();
		logger.info("Processing MGREP to DAT Table in:"
				+ timer.millisecondsToTimeString(timer.duration()) + "\n");

		// Deletes the temporary table
		StringBuffer deleteQuery = new StringBuffer();
		deleteQuery.append("DROP TABLE ");
		deleteQuery.append(this.getTableSQLName());
		deleteQuery.append("_MGREP;");
		try{
			this.executeSQLUpdate(deleteQuery.toString());
			}
		catch(SQLException e){
			logger.error("** PROBLEM ** Cannot delete the temporary table.", e);
		}
		return nbAnnotation;
	}

	//********************************* DELETE FUNCTIONS *****************************************************/

	private void openDeleteEntriesFromOntologyStatement(){
		// Query Used :
		//	DELETE DAT FROM obr_tr_annotation DAT, obs_concept CT, obs_ontology OT
		//		WHERE DAT.conept_id = CT.id
		//			AND CT.ontology_id = OT.id
		//			AND OT.local_ontology_id = ?;
		StringBuffer queryb = new StringBuffer();
		queryb.append("DELETE DAT FROM ");
		queryb.append(this.getTableSQLName());
		queryb.append(" DAT, ");
		queryb.append(conceptDao.getMemoryTableSQLName( ));
		queryb.append(" CT, ");
		queryb.append(ontologyDao.getMemoryTableSQLName());
		queryb.append(" OT ");
		queryb.append(" WHERE DAT.concept_id = CT.id AND CT.ontology_id = OT.id AND OT.local_ontology_id = ?");

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
	 * Deletes the rows corresponding to annotations done with a concept in the given list of localOntologyIDs.
	 *
	 * @param {@code List} of local ontology ids
	 * @return True if the rows were successfully removed.
	 */
	public boolean deleteEntriesFromOntologies(List<String> localOntologyIDs){
		boolean deleted = false;
		StringBuffer queryb = new StringBuffer();

		/*queryb.append("DELETE DAT FROM ");
		queryb.append(this.getTableSQLName());
		queryb.append(" DAT, ");
		queryb.append(conceptDao.getMemoryTableSQLName( ));
		queryb.append(" CT, ");
		queryb.append(ontologyDao.getMemoryTableSQLName());
		queryb.append(" OT ");
		queryb.append(" WHERE DAT.concept_id = CT.id AND CT.ontology_id = OT.id AND OT.local_ontology_id IN (");

		for (String localOntologyID : localOntologyIDs) {
			queryb.append("'");
			queryb.append(localOntologyID);
			queryb.append("', ");
		}
		queryb.delete(queryb.length()-2, queryb.length());
		queryb.append(");");
		*/


		queryb.append("DELETE DAT FROM ");
		queryb.append(this.getTableSQLName());
		queryb.append(" DAT ");
		queryb.append(" WHERE DAT.concept_id IN ( ");
		queryb.append(" SELECT id FROM   ");
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

	/**
	 * Deletes the rows corresponding to annotations done with a termName in the given String list.
	 * @return Number of rows deleted.
	 */
	public long deleteEntriesFromStopWords(HashSet<String> stopwords){
		long nbDelete = -1;
		/* DELETE OBR_GEO_DAT FROM OBR_GEO_DAT, OBS_TT
		WHERE OBR_GEO_DAT.termID=OBS_TT.termID AND termName IN ();*/
		StringBuffer queryb = new StringBuffer();
		queryb.append("DELETE ");
		queryb.append(this.getTableSQLName());
		queryb.append(" FROM ");
		queryb.append(this.getTableSQLName());
		queryb.append(",");
		queryb.append(termDao.getTableSQLName());
		queryb.append(" WHERE ");
		queryb.append(this.getTableSQLName());
		queryb.append(".term_id=");
		queryb.append(termDao.getTableSQLName());
		queryb.append(".id AND ");
		queryb.append(" name IN(");
		for(Iterator<String> it = stopwords.iterator(); it.hasNext();){
			queryb.append("'");
			queryb.append(it.next());
			queryb.append("'");
			if(it.hasNext()){
				queryb.append(", ");
			}
		}
		queryb.append(");");
		try {
			if(stopwords.size()>0){
				nbDelete = this.executeSQLUpdate(queryb.toString());
				this.closeTableGenericStatement();
			}
			else{
				nbDelete = 0;
			}
		}
		catch (SQLException e) {
			logger.error("** PROBLEM ** Cannot delete entries from "+this.getTableSQLName()+" with given list of stopwords. -1 returned.", e);
		}
		return nbDelete;
	}

	//**********************Annotations Statistics ******************/

	/**
	 *
	 *  Get number of Mgrep Annotations for each ontlogyID
	 * @param dictionary
	 * @param withCompleteDictionary
	 *
	 *  @return Map containing number of mgerp annotations for each ontology as key.
	 *
	 */
	public HashMap<Integer, Long> getMgrepAnnotationStatistics(boolean withCompleteDictionary, DictionaryBean dictionary){
		HashMap<Integer, Long> annotationStats = new HashMap<Integer, Long>();

		StringBuffer queryb = new StringBuffer();
		if(withCompleteDictionary){
			queryb.append("SELECT CT.ontology_id, COUNT(DAT.concept_id) AS COUNT FROM ");
			queryb.append(this.getTableSQLName());
			queryb.append(" AS DAT, ");
			queryb.append(conceptDao.getMemoryTableSQLName());
			queryb.append(" AS CT WHERE DAT.concept_id=CT.id AND DAT.term_id IS NOT NULL GROUP BY CT.ontology_id; ");
		}else{
			queryb.append("SELECT OT.id, COUNT(DAT.concept_id) AS COUNT FROM ");
			queryb.append(this.getTableSQLName());
			queryb.append(" AS DAT, ");
			queryb.append(conceptDao.getMemoryTableSQLName());
			queryb.append(" AS CT, ");
			queryb.append(ontologyDao.getMemoryTableSQLName());
			queryb.append(" AS OT WHERE DAT.concept_id=CT.id AND CT.ontology_id=OT.id AND DAT.term_id IS NOT NULL AND OT.dictionary_id = ");
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
			return this.getMgrepAnnotationStatistics(withCompleteDictionary, dictionary);
		}
		catch (SQLException e) {
			logger.error("** PROBLEM ** Cannot get mgrep annotations statistics from "+this.getTableSQLName()+" .", e);
		}
		return annotationStats;

	}

	/**
	 *  Get number of reported annotations for each ontlogyID
	 * @param dictionary
	 * @param withCompleteDictionary
	 *
	 *  @return Map containing number of reported annotations for each ontology as key.
	 */
	public HashMap<Integer, Long> getReportedAnnotationStatistics(boolean withCompleteDictionary, DictionaryBean dictionary){
		HashMap<Integer, Long> annotationStats = new HashMap<Integer, Long>();

		StringBuffer queryb = new StringBuffer();
		if(withCompleteDictionary){
			queryb.append("SELECT CT.ontology_id, COUNT(DAT.concept_id) AS COUNT FROM ");
			queryb.append(this.getTableSQLName());
			queryb.append(" AS DAT, ");
			queryb.append(conceptDao.getMemoryTableSQLName());
			queryb.append(" AS CT WHERE DAT.concept_id=CT.id AND DAT.term_id IS NULL GROUP BY CT.ontology_id; ");
		}else{
			queryb.append("SELECT OT.id, COUNT(DAT.concept_id) AS COUNT FROM ");
			queryb.append(this.getTableSQLName());
			queryb.append(" AS DAT, ");
			queryb.append(conceptDao.getMemoryTableSQLName());
			queryb.append(" AS CT, ");
			queryb.append(ontologyDao.getMemoryTableSQLName());
			queryb.append(" AS OT WHERE DAT.concept_id=CT.id AND CT.ontology_id=OT.id AND DAT.term_id IS NULL AND OT.dictionary_id = ");
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
			return this.getReportedAnnotationStatistics(withCompleteDictionary, dictionary);
		}
		catch (SQLException e) {
			logger.error("** PROBLEM ** Cannot get reported annotations statistics from " +this.getTableSQLName()+" .", e);
		}
		return annotationStats;

	}

	public boolean isIndexExist(){
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

	public boolean createIndex() {
		boolean result = false;
		try{
			this.executeSQLUpdate(getIndexCreationQuery());
			result = true;
			}
		catch(SQLException e){
			logger.error("** PROBLEM ** Cannot delete the temporary table.", e);
		}
		 return result;
	}

	/********************************* ENTRY CLASSES *****************************************************/

	/**
	 * This class is a representation for a OBR_XX_DAT table entry.
	 *
	 * @author Adrien Coulet, Clement Jonquet
	 * @version OBR_v0.2
	 * @created 12-Nov-2008
	 */
	public static class DirectAnnotationEntry {

		private String localElementID;
		private String localConceptID;
		private String contextName;
		private Integer dictionaryID;
		private Integer workflowStatus;


		public DirectAnnotationEntry(String localElementID, String localConceptID, String contextName, Integer dictionaryID,
				 Integer workflowStatus) {
			super();
			this.localElementID = localElementID;
			this.localConceptID = localConceptID;
			this.contextName = contextName;
			this.dictionaryID = dictionaryID;
			this.workflowStatus =workflowStatus;
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

		public Integer getDictionaryId() {
			return dictionaryID;
		}

		public Integer getWorkflowStatus() {
			return workflowStatus;
		}

		public String toString(){
			StringBuffer sb = new StringBuffer();
			sb.append("DirectAnnotationEntry: [");
			sb.append(this.localElementID);
			sb.append(", ");
			sb.append(this.localConceptID);
			sb.append(", ");
			sb.append(this.contextName);
			sb.append(", ");
			sb.append(this.dictionaryID);
			sb.append(", ");
			sb.append(this.workflowStatus);
			sb.append("]");
			return sb.toString();
		}
	}


	/**
	 * This class is a representation for a OBR_XX_DAT table entry.
	 * This class corresponds to an annotation done with Mgrep.
	 *
	 * @author Adrien Coulet, Clement Jonquet
	 * @version OBR_v0.2
	 * @created 12-Nov-2008
	 */
	static class DirectMgrepAnnotationEntry extends DirectAnnotationEntry {

		private Integer termID;
		private Integer from;
		private Integer to;

		public DirectMgrepAnnotationEntry(String localElementID,
				String localConceptID, String contextName, Integer termID,
				Integer from, Integer to, Integer dictionaryID,
				Integer workflowStaus) {
			super(localElementID, localConceptID, contextName, dictionaryID, workflowStaus);
			this.termID = termID;
			this.from = from;
			this.to = to;
		}

		public Integer getTermID() {
			return termID;
		}

		public Integer getFrom() {
			return from;
		}

		public Integer getTo() {
			return to;
		}

		public String toString(){
			StringBuffer sb = new StringBuffer();
			sb.append("DirectMgrepAnnotationEntry: [");
			sb.append(this.getLocalElementId());
			sb.append(", ");
			sb.append(this.getLocalConceptID());
			sb.append(", ");
			sb.append(this.getContextName());
			sb.append(", ");
			sb.append(this.termID);
			sb.append(", ");
			sb.append(this.from);
			sb.append(", ");
			sb.append(this.to);
			sb.append(", ");
			sb.append(this.getDictionaryId());
			sb.append(", ");
			sb.append(this.getWorkflowStatus());
			sb.append("]");
			return sb.toString();
		}
	}


}

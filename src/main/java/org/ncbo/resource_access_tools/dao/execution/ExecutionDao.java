package org.ncbo.resource_access_tools.dao.execution;

import com.mysql.jdbc.exceptions.MySQLIntegrityConstraintViolationException;
import com.mysql.jdbc.exceptions.MySQLNonTransientConnectionException;
import org.ncbo.resource_access_tools.dao.AbstractObrDao;
import org.ncbo.resource_access_tools.util.MessageUtils;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Date;

/**
 * This class is a representation for the the obr_execution table.
 *
 * @author Kuladip Yadav
 *
 */
public class ExecutionDao extends AbstractObrDao {

	// Table suffix string
	private static final String TABLE_SUFFIX = MessageUtils.getMessage("obr.execution.table.suffix");

	// Prepared statement for adding new entry.
	private static PreparedStatement addEntryStatement;

	/**
	 * Default constructor
	 */
	private ExecutionDao() {
		super(EMPTY_STRING, TABLE_SUFFIX);
	}

	public static String name(){
		return OBR_PREFIX + TABLE_SUFFIX;
	}

	@Override
	protected String creationQuery(){
		return "CREATE TABLE " + getTableSQLName() +" (" +
					"id SMALLINT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY, " +
					"resource_id INT UNSIGNED NOT NULL, " +
					"dictionary_id SMALLINT UNSIGNED, "+
					"with_complete_dictionary TINYINT(1) UNSIGNED NOT NULL, " +
					"nb_element INT UNSIGNED, "+
					"first_execution TINYINT(1) UNSIGNED NOT NULL, " +
					"execution_beginning TIMESTAMP NULL DEFAULT NULL, " +
					"execution_end TIMESTAMP NULL DEFAULT NULL, " +
					"execution_time TIME  NULL DEFAULT NULL " +
				  ") ENGINE=MyISAM DEFAULT CHARSET=latin1;";
	}

	@Override
	protected void openPreparedStatements() {
		super.openPreparedStatements();
		this.openAddEntryStatement();
	}

	@Override
	protected void closePreparedStatements() throws SQLException {
		super.closePreparedStatements();
		addEntryStatement.close();

	}

	private static class ExecutionDaoHolder {
		private final static ExecutionDao EXECUTION_DAO_INSTANCE = new ExecutionDao();
	}

	/**
	 * Returns a ExecutionDao object by creating one if a singleton not already exists.
	 */
	public static ExecutionDao getInstance(){
		return ExecutionDaoHolder.EXECUTION_DAO_INSTANCE;
	}

	/****************************************** FUNCTIONS ON THE TABLE ***************************/

	@Override
	protected void openAddEntryStatement(){
		StringBuffer queryb = new StringBuffer();
		queryb.append("INSERT INTO ");
		queryb.append(this.getTableSQLName());
		queryb.append(" ( resource_id, dictionary_id, with_complete_dictionary, nb_element, first_execution, execution_beginning, execution_end, execution_time) ");
		queryb.append(" VALUES ");
		queryb.append(" ((SELECT RT.id FROM ");
		queryb.append(resourceTableDao.getTableSQLName());
		queryb.append(" RT WHERE RT.resource_id= ?), ?, ?, ?, ?, ?, ?, TIMEDIFF(?,?) )");
		addEntryStatement = this.prepareSQLStatement(queryb.toString());
	}

	/**
	 * Add an new entry in corresponding SQL table.
	 * @return True if the entry was added to the SQL table, false if a problem occurred during insertion.
	 */
	public boolean addEntry(ExecutionEntry executionEntry){
		boolean inserted = false;
		try {
			addEntryStatement.setString(1, executionEntry.getResourceId());
			addEntryStatement.setInt(2, executionEntry.getDictionaryId());
			addEntryStatement.setBoolean(3, executionEntry.isWithCompleteDictionary());
			addEntryStatement.setInt(4, executionEntry.getNbElement());
			addEntryStatement.setBoolean(5, executionEntry.isFirstExecution());
			addEntryStatement.setTimestamp(6, new java.sql.Timestamp(executionEntry.getExecutionBeginning().getTime()));
			addEntryStatement.setTimestamp(7,  new java.sql.Timestamp(executionEntry.getExecutionEnd().getTime()));
			addEntryStatement.setTimestamp(8,  new java.sql.Timestamp(executionEntry.getExecutionEnd().getTime()));
			addEntryStatement.setTimestamp(9,  new java.sql.Timestamp(executionEntry.getExecutionBeginning().getTime()));

			this.executeSQLUpdate(addEntryStatement);
			inserted = true;
		}
		catch (MySQLNonTransientConnectionException e) {
			this.openAddEntryStatement();
			return this.addEntry(executionEntry);
		}
		catch (MySQLIntegrityConstraintViolationException e){
			//logger.error("Table " + this.getTableSQLName() + " already contains an entry for the concept: " + entry.getLocalConceptID() +".");
		}
		catch (SQLException e) {
			logger.error("** PROBLEM ** Cannot add an entry on table " + this.getTableSQLName(), e);
			logger.error(executionEntry.toString());
		}
		return inserted;
	}


	/**
	 * This class is a representation for a obr_execution table entry.
	 *
	 * @author Kuladip Yadav
	 *
	 */
	public static class ExecutionEntry {

		private String resourceId;
		private int dictionaryId;
		private boolean withCompleteDictionary;
		private int nbElement;
		private boolean firstExecution;
		private Date executionBeginning;
		private Date executionEnd;
		private long executionTime;

		public ExecutionEntry(){
			super();
		}

		public ExecutionEntry(String resourceId, int dictionaryId,
				boolean withCompleteDictionary, int nbElement,
				boolean firstExecution, Date executionBeginning,
				Date executionEnd, long executionTime) {
			super();
			this.resourceId = resourceId;
			this.dictionaryId = dictionaryId;
			this.withCompleteDictionary = withCompleteDictionary;
			this.nbElement = nbElement;
			this.firstExecution = firstExecution;
			this.executionBeginning = executionBeginning;
			this.executionEnd = executionEnd;
			this.executionTime = executionTime;
		}

		public String getResourceId() {
			return resourceId;
		}

		public void setResourceId(String resourceId) {
			this.resourceId = resourceId;
		}

		public int getDictionaryId() {
			return dictionaryId;
		}

		public void setDictionaryId(int dictionaryId) {
			this.dictionaryId = dictionaryId;
		}

		public boolean isWithCompleteDictionary() {
			return withCompleteDictionary;
		}

		public void setWithCompleteDictionary(boolean withCompleteDictionary) {
			this.withCompleteDictionary = withCompleteDictionary;
		}

		public int getNbElement() {
			return nbElement;
		}

		public void setNbElement(int nbElement) {
			this.nbElement = nbElement;
		}

		public boolean isFirstExecution() {
			return firstExecution;
		}

		public void setFirstExecution(boolean firstExecution) {
			this.firstExecution = firstExecution;
		}

		public Date getExecutionBeginning() {
			return executionBeginning;
		}

		public void setExecutionBeginning(Date executionBeginning) {
			this.executionBeginning = executionBeginning;
		}

		public Date getExecutionEnd() {
			return executionEnd;
		}

		public void setExecutionEnd(Date executionEnd) {
			this.executionEnd = executionEnd;
		}

		public long getExecutionTime() {
			return executionTime;
		}

		public void setExecutionTime(long executionTime) {
			this.executionTime = executionTime;
		}

		public String toString(){
			StringBuffer sb = new StringBuffer();
			sb.append("ExecutionEntry: [ resourceId ");
			sb.append(this.resourceId);
			sb.append(", dictionaryId ");
			sb.append(this.dictionaryId);
			sb.append(", withCompleteDictionary ");
			sb.append(this.withCompleteDictionary);
			sb.append(", nbElement ");
			sb.append(this.nbElement);
			sb.append(", firstExecution ");
			sb.append(this.firstExecution);
			sb.append(", executionBeginning ");
			sb.append(this.executionBeginning);
			sb.append(", executionEnd ");
			sb.append(this.executionEnd);
			sb.append(", executionTime ");
			sb.append(this.executionTime);
			sb.append("]");
			return sb.toString();
		}


	}


}

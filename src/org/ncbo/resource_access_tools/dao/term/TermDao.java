package org.ncbo.resource_access_tools.dao.term;

import java.io.File;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.ncbo.resource_access_tools.common.utils.Utilities;
import org.ncbo.resource_access_tools.dao.concept.ConceptDao;
import org.ncbo.resource_access_tools.dao.obs.AbstractObsDao;
import org.ncbo.resource_access_tools.dao.ontology.OntologyDao;
import org.ncbo.resource_access_tools.util.FileResourceParameters;
import org.ncbo.resource_access_tools.util.MessageUtils;
import org.ncbo.resource_access_tools.util.StringUtilities;

import com.mysql.jdbc.exceptions.MySQLNonTransientConnectionException;
/**
 * This class is a representation for the OBS(slave) DB obs_term table. The table contains 
 * the following columns:
 * <ul>
 * <li>id INT(11) NOT NULL PRIMARY KEY
   <li>name TEXT NOT NULL
   <li>concept_id INT(11) NOT NULL
   <li>dictionary_id INT(11) NOT NULL
 * </ul>
 * 
 */
public class TermDao extends AbstractObsDao{
	
	private static final String TABLE_SUFFIX = MessageUtils.getMessage("obs.term.table.suffix");
 
	private PreparedStatement addEntryStatement;
	private PreparedStatement exactMapStringToLocalConceptIDsStatement;
	private static PreparedStatement deleteEntriesFromOntologyStatement;

	private TermDao() {
		super(TABLE_SUFFIX);

	}
	
	private static class TermDaoHolder {
		private final static TermDao TERM_DAO_INSTANCE = new TermDao ();
	}

	/**
	 * Returns a TermTable object by creating one if a singleton not already exists.
	 */
	public static TermDao getInstance(){
		return TermDaoHolder.TERM_DAO_INSTANCE;
	}

	
	public static String name(){		
		return OBS_PREFIX + TABLE_SUFFIX;
	}

	@Override
	protected void openPreparedStatements() {
		super.openPreparedStatements();
		this.openAddEntryStatement();	
		this.openExactMapStringToLocalConceptIDsStatement();
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
		queryb.append(" (id, name, concept_id, is_preferred) VALUES (?,?,?,?);");
		addEntryStatement = this.prepareSQLStatement(queryb.toString());
	}
	
	@Override
	protected String creationQuery() {
		//need to change this query for dictionary_id
		return "CREATE TABLE " + getTableSQLName() +" (" +
		"id INT(11) NOT NULL PRIMARY KEY, " +
		"name TEXT NOT NULL, " +
		"concept_id INT(11) NOT NULL, " +
		"is_preferred TINYINT(1) NOT NULL, " +		 
		"INDEX X_" + getTableSQLName() +"_termName (name(255)), " +
		"INDEX X_" + getTableSQLName() +"_concept_id (concept_id), " +
		"INDEX X_" + getTableSQLName() +"_isPreferred (is_preferred)" +
		") ENGINE=MyISAM DEFAULT CHARSET=latin1;";
	}
	
	/**
	 * Add a new entry in corresponding(here, obs_term) SQL table.
	 * @return True if the entry was added to the SQL table, false if a problem occurred during insertion.
	 */
	public boolean addEntry(TermEntry entry){
		boolean inserted = false;
		try {
			addEntryStatement.setInt(1, entry.getId());
			addEntryStatement.setString(2, StringUtilities.escapeLine(entry.getName()));
			addEntryStatement.setInt(3, entry.getConceptID());
			addEntryStatement.setBoolean(4, entry.isPreferred());		
	
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
	
	/******************* Term Table related query*********************/
	
	private void openExactMapStringToLocalConceptIDsStatement(){
		StringBuffer queryb = new StringBuffer();
		queryb.append(mapStringQueries());
		queryb.append(" AND ");
		queryb.append(this.getTableSQLName());
		queryb.append(".name=? AND local_ontology_id=?;");
		exactMapStringToLocalConceptIDsStatement = this.prepareSQLStatement(queryb.toString());
	} 
	
	private String mapStringQueries(){
		StringBuffer queryb = new StringBuffer();
		queryb.append("SELECT local_concept_id FROM ");
		queryb.append(this.getTableSQLName());
		queryb.append(", ");
		queryb.append(conceptDao.getTableSQLName());
		queryb.append(", ");
		queryb.append(ontologyDao.getTableSQLName());
		queryb.append(" WHERE ");
		queryb.append(this.getTableSQLName());
		queryb.append(".concept_id=");
		queryb.append(conceptDao.getTableSQLName());
		queryb.append(".id AND ");
		queryb.append(conceptDao.getTableSQLName());
		queryb.append(".ontology_id=");
		queryb.append(ontologyDao.getTableSQLName());
		queryb.append(".id");
		return queryb.toString();
	}
	
	public HashSet<String> mapStringToLocalConceptIDs(String s, String localOntologyID){
		HashSet<String> localConceptIDs = new HashSet<String>();
		try {
			ResultSet rSet;			 
			exactMapStringToLocalConceptIDsStatement.setString(1, s);
			exactMapStringToLocalConceptIDsStatement.setString(2, localOntologyID);
			rSet = this.executeSQLQuery(exactMapStringToLocalConceptIDsStatement);
			 
			 
			while(rSet.next()){
				localConceptIDs.add(rSet.getString(1));
			}
			rSet.close();
		}
		catch (MySQLNonTransientConnectionException e) {
			this.openExactMapStringToLocalConceptIDsStatement();
			 
			return this.mapStringToLocalConceptIDs(s, localOntologyID);
		}
		catch (SQLException e) {
			logger.error("** PROBLEM ** Cannot get concepts from "+this.getTableSQLName()+" that map string: "+ s +" in ontology: "+localOntologyID+". Empty set returned.", e);
		}
		return localConceptIDs;
	} 

	/**
	 *  
	 * @param terms
	 * @param localOntologyID
	 * @return
	 */
	public HashSet<String> mapTermsToLocalConceptIDs(Set<String> terms, String localOntologyID){
		HashSet<String> localConceptIDs = new HashSet<String>();
		
		if(terms== null || terms.isEmpty()){
			return localConceptIDs;
		}
		
		try {
			ResultSet rSet;	 
			StringBuffer queryb = new StringBuffer();
			queryb.append(mapStringQueries());
			queryb.append(" AND ");
			queryb.append(this.getTableSQLName());
			queryb.append(".name IN (");
			for(Iterator<String> it = terms.iterator(); it.hasNext();){
				queryb.append("'");
				queryb.append(it.next().replace("'", "\\'"));
				queryb.append("'");
				if(it.hasNext()){
					queryb.append(", ");
				}
			}
			queryb.append(") AND local_ontology_id= ");
			queryb.append(localOntologyID);
			queryb.append(";");
			
			rSet = this.executeSQLQuery(queryb.toString());
			 
			while(rSet.next()){
				localConceptIDs.add(rSet.getString(1));
			}
			rSet.close();
		}
		catch (MySQLNonTransientConnectionException e) {
			 return this.mapTermsToLocalConceptIDs(terms, localOntologyID);
		}
		catch (SQLException e) {
			logger.error("** PROBLEM ** Cannot get concepts from "+this.getTableSQLName()+" that map string: "+ terms +" in ontology: "+localOntologyID+". Empty set returned.", e);
		}
		return localConceptIDs;
	}
	
	/**
	 * Method loads the data entries from given file to term table.
	 * 
	 * @param termEntryFile File containing term table entries.
	 * @return Number of entries populated in term table.
	 */
	public long populateSlaveTermTableFromFile(File termEntryFile) {
		StringBuffer queryb = new StringBuffer();
		queryb.append("LOAD DATA LOCAL INFILE '");
		queryb.append(termEntryFile.getAbsolutePath());
		queryb.append("' IGNORE INTO TABLE ");
		queryb.append(this.getTableSQLName());
		queryb.append(" FIELDS TERMINATED BY '\t' IGNORE 1 LINES");		
		long nbInserted =0;
		try{
			 nbInserted = this.executeSQLUpdate(queryb.toString());
			
		} catch (SQLException e) {			 
			logger.error("Problem in populating term table from file : " + termEntryFile.getAbsolutePath(), e);
		}	
		return nbInserted;
	}
	private void openDeleteEntriesFromOntologyStatement(){	 
		// Query Used :
		//	DELETE TT FROM obs_term TT, obs_concept CT, obs_ontology OT
		//		WHERE TT.concept_id = CT.id
		//			AND CT.ontology_id = OT.id
		//			AND OT.local_ontology_id = ?;		
		StringBuffer queryb = new StringBuffer();
		queryb.append("DELETE TT FROM ");
		queryb.append(this.getTableSQLName());		
		queryb.append(" TT ");
		queryb.append(" WHERE TT.concept_id in (");
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
		
		
		/*queryb.append("DELETE TT FROM ");
		queryb.append(this.getTableSQLName());		
		queryb.append(" TT, ");
		queryb.append(ConceptDao.name( ));	
		queryb.append(" CT, ");
		queryb.append(OntologyDao.name());
		queryb.append(" OT ");
		queryb.append(" WHERE TT.concept_id = CT.id AND CT.ontology_id = OT.id AND OT.local_ontology_id = ?");*/
		 		
		deleteEntriesFromOntologyStatement = this.prepareSQLStatement(queryb.toString());
	}
	/**
	 * Deletes the rows for the given local_ontology_id.
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
	 * Deletes the rows corresponding to annotations done with a termName in the given String list.
	 * @return Number of rows deleted. 
	 */
	public long deleteEntriesForStopWords(){		
		long nbDelete = -1; 
		HashSet<String> stopwords= Utilities.arrayToHashSet(FileResourceParameters.STOP_WORDS);
		/* DELETE obs_term FROM obs_term WHERE obs_term.name IN();*/
		StringBuffer queryb = new StringBuffer();
		queryb.append("DELETE ");
		queryb.append(this.getTableSQLName());
		queryb.append(" FROM ");
		queryb.append(this.getTableSQLName());		
		queryb.append(" WHERE name IN(");
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
	
	
	/**
	 * This class is representation for obs_term table entry.
	 * @author  
	 *
	 */
	public static class TermEntry{

		private int id;
		private String name;
		private int conceptID;
		private boolean isPreferred;		 

		public TermEntry(int id, String name, int conceptID,
				boolean isPreferred) {
			this.id = id;
			this.name = name;
			this.conceptID = conceptID;
			this.isPreferred = isPreferred;			 
		}

		public int getId() {
			return id;
		}
		public void setId(int id) {
			this.id = id;
		}
		public String getName() {
			return name;
		}
		public void setName(String name) {
			this.name = name;
		}
		public int getConceptID() {
			return conceptID;
		}
		public void setConceptID(int conceptID) {
			this.conceptID = conceptID;
		}
		public boolean isPreferred() {
			return isPreferred;
		}
		public void setPreferred(boolean isPreferred) {
			this.isPreferred = isPreferred;
		}
		 
		public String toString(){
			StringBuffer sb = new StringBuffer();
			sb.append("TermEntry: [");
			sb.append(this.id);
			sb.append(", ");			
			sb.append(this.name);
			sb.append(", ");
			sb.append(this.conceptID);
			sb.append(", ");
			sb.append(this.isPreferred);			 
			sb.append("]");
			return sb.toString();
		}
	} 
}

package org.ncbo.resource_access_tools.dao.dictionary;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Calendar;

import org.ncbo.resource_access_tools.common.beans.DictionaryBean;
import org.ncbo.resource_access_tools.dao.AbstractObrDao;
import org.ncbo.resource_access_tools.dao.term.TermDao;
import org.ncbo.resource_access_tools.util.FileResourceParameters;
import org.ncbo.resource_access_tools.util.MessageUtils;

import com.mysql.jdbc.exceptions.MySQLIntegrityConstraintViolationException;
import com.mysql.jdbc.exceptions.MySQLNonTransientConnectionException;

/**
* This class is a representation for the the OBS DB OBS_DVT table. The table contains 
* the following columns:
* <ul>
 * <li> dictionaryID 	SMALLINT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY,
 * <li> dictionaryName 	CHAR(13) NOT NULL UNIQUE),
 * <li> dictionaryDate 	DATETIME NOT NULL.
* </ul>
*  
* @author Clement Jonquet
* @version OBS_v1		
* @created 25-Sept-2008
*
*/
public class DictionaryDao extends AbstractObrDao {

	protected static final String TABLE_SUFFIX = MessageUtils.getMessage("obr.dictionary.table.suffix");
	
	private static PreparedStatement addEntryStatement;
	private static PreparedStatement getLastDictionaryBeanStatement;
	private static PreparedStatement deleteEntryStatement;
	
	private DictionaryDao() {
		super(EMPTY_STRING, TABLE_SUFFIX );
	}
	
	@Override
	protected String creationQuery(){
		return "CREATE TABLE " + this.getTableSQLName() +" (" +
					"id SMALLINT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY, " +
					"name CHAR(13) NOT NULL UNIQUE, " +
					"date_created DATETIME" +
				") ENGINE=MyISAM DEFAULT CHARSET=latin1;";
	}
	
	@Override
	protected void openPreparedStatements(){
		super.openPreparedStatements();
		this.openAddEntryStatement();
		this.openDeleteEntryStatement();
		this.openGetLastDictionaryBeanStatement();		 
	}
	
	@Override
	protected void closePreparedStatements() throws SQLException {
		super.closePreparedStatements();
		addEntryStatement.close();
		getLastDictionaryBeanStatement.close();
		deleteEntryStatement.close();
	}
	
	private static class DictionaryDaoHolder {
		private final static DictionaryDao DICTIOANRY_DAO_INSTANCE = new DictionaryDao();
	}

	/**
	 * Returns a DictionaryDao object by creating one if a singleton not already exists.
	 */
	public static DictionaryDao getInstance(){
		return DictionaryDaoHolder.DICTIOANRY_DAO_INSTANCE;
	}
	
	/****************************************** FUNCTIONS ON THE TABLE ***************************/ 
	
	@Override
	protected void openAddEntryStatement(){
		StringBuffer queryb = new StringBuffer();
		queryb.append("INSERT INTO ");
		queryb.append(this.getTableSQLName());
		queryb.append(" (name, date_created) VALUES (?,NOW());");
		addEntryStatement = this.prepareSQLStatement(queryb.toString());
	}
	
 
	protected void openDeleteEntryStatement(){
		StringBuffer queryb = new StringBuffer();
		queryb.append("DELETE DT FROM ");
		queryb.append(this.getTableSQLName());
		queryb.append(" DT WHERE DT.id= ? ;");
		deleteEntryStatement = this.prepareSQLStatement(queryb.toString());
	}
	
	/**
	 * Add an new entry in corresponding SQL table.
	 * @return True if the entry was added to the SQL table, false if a problem occurred during insertion.
	 */
	public boolean addEntry(String dictionaryName){
		boolean inserted = false;
		try {
			addEntryStatement.setString(1, dictionaryName);
			this.executeSQLUpdate(addEntryStatement);
			inserted = true;
		}
		catch (MySQLNonTransientConnectionException e) {
			this.openAddEntryStatement();
			return this.addEntry(dictionaryName);
		}
		catch (MySQLIntegrityConstraintViolationException e){
			logger.error("Table " + this.getTableSQLName() + " already contains an entry for dictionaryName: " + dictionaryName +".");
		}
		catch (SQLException e) {
			logger.error("** PROBLEM ** Cannot add an entry on table " + this.getTableSQLName(), e);
			logger.error(dictionaryName);
		}
		return inserted;	
	}
	
	/**
	 * Add an new entry in corresponding SQL table.
	 * @return True if the entry was added to the SQL table, false if a problem occurred during insertion.
	 */
	public boolean deleteEntry(int dictionaryID){
		boolean deleted = false;
		try {
			deleteEntryStatement.setInt(1, dictionaryID);
			this.executeSQLUpdate(deleteEntryStatement);
			deleted = true;
		}
		catch (MySQLNonTransientConnectionException e) {
			this.openDeleteEntryStatement();
			return this.deleteEntry(dictionaryID);
		} 
		catch (SQLException e) {
			logger.error("** PROBLEM ** Cannot delete an entry on table " + this.getTableSQLName(), e);			 
		}
		return deleted;	
	}
	
	private void openGetLastDictionaryBeanStatement(){
		StringBuffer queryb = new StringBuffer();
		queryb.append("SELECT id, name, date_created  FROM ");
		queryb.append(this.getTableSQLName());
		queryb.append(" WHERE id =(SELECT MAX(id) FROM ");
		queryb.append(this.getTableSQLName());
		queryb.append(");");
		getLastDictionaryBeanStatement = this.prepareSQLStatement(queryb.toString());
	}
	
	public DictionaryBean getLastDictionaryBean(){
		DictionaryBean dictionary;
		try {
			ResultSet rSet = this.executeSQLQuery(getLastDictionaryBeanStatement);
			if(rSet.first()){
				Calendar cal = Calendar.getInstance();
				cal.setTime(rSet.getDate(3));
				dictionary = new DictionaryBean(rSet.getInt(1), rSet.getString(2), cal);
			}
			else{
				dictionary = null;
			}
			rSet.close();
		}
		catch (MySQLNonTransientConnectionException e) {
			this.openGetLastDictionaryBeanStatement();
			return this.getLastDictionaryBean();
		}
		catch (SQLException e) {
			logger.error("** PROBLEM ** Cannot get last dictionary from "+this.getTableSQLName()+". Null returned.", e);
			dictionary = null;
		}
		return dictionary;
	} 

	
	/*
	 * Moving methods from ObsOntologiesAccessTool
	 * 
	 */
	
	public static String dictionaryFileName(DictionaryBean dictionary){
		logger.info("Mgrep file created ::"+FileResourceParameters.dictionaryFolder()+dictionary.getDictionaryName()+"_MGREP.txt");
		return FileResourceParameters.dictionaryFolder() + dictionary.getDictionaryName() + "_MGREP.txt";
	}
	
	public static String completeDictionaryFileName(DictionaryBean dictionary){
		return FileResourceParameters.dictionaryFolder() + dictionary.getDictionaryName() + "_CMP_MGREP.txt";
	}
	
	/**
	 * Adds to the query to create the dictionary a restriction on the terms selected 
	 * according to a given blacklist.
	 */
	private String blackListFilter(){
		// Specify the black list to use here
		//String blacklist = "OBS_MGREP_basics.txt";
		String blacklist = "OBS_MGREP_empty.txt";
		StringBuffer sb = new StringBuffer();
		sb.append("name NOT IN(''");
		// reads the black list file
		File blackFile = new File(FileResourceParameters.blackListFolder() + blacklist);
		try{
		FileReader fstream = new FileReader(blackFile);
		BufferedReader in = new BufferedReader(fstream);
		String line = in.readLine();
		while (line != null){
			sb.append(", '");
			sb.append(line);
			sb.append("'");
			line = in.readLine();
		}
		in.close();
		fstream.close();
		}
		catch (IOException e) {
			logger.error("** PROBLEM ** Cannot read balck list to filter the dictionary.", e);
		}
		sb.append(")");
		return sb.toString();
	}
	
	/**
	 * Write the given file with the [id name] couples present in the corresponding SQL table
	 * for a given dictionaryID. Used to generate a dictionary file for Mgrep.
	 * @return The number of lines written in the given file.
	 */
	public long writeDictionaryFile(File file, int dictionaryID){
		StringBuffer queryb = new StringBuffer();		 
		queryb.append("SELECT TT.id, TT.name FROM ");
		queryb.append(termDao.getTableSQLName());
		queryb.append(" TT, ");
		queryb.append(conceptDao.getMemoryTableSQLName());
		queryb.append(" CT, ");
		queryb.append(ontologyDao.getMemoryTableSQLName());
		queryb.append(" OT WHERE TT.concept_id=CT.id AND CT.ontology_id=OT.id AND TT.");		 
		queryb.append(this.blackListFilter());
		queryb.append(" AND OT.dictionary_id = ");
		queryb.append(dictionaryID);
		queryb.append("; "); 
		
		long nbLines = 0;
		try{
			ResultSet couplesSet = this.executeSQLQuery(queryb.toString());
			nbLines = this.writeFile(file, couplesSet);
			couplesSet.close();
			this.closeTableGenericStatement();
		}
		catch(Exception e){
			logger.error("** PROBLEM ** Cannot write dictionary file " + file.getName()+" with dictionaryID: " + dictionaryID, e);
		}
		return nbLines;
	}
	
	/**
	 * Write the given file with all the [id name] couples present in the corresponding SQL table.
	 * Used to generate a complete dictionary file for Mgrep.
	 * @return The number of lines written in the given file.
	 */
	public long writeDictionaryFile(File file){
		
		StringBuffer queryb = new StringBuffer();		 
		queryb.append("SELECT id, name FROM ");
		queryb.append(TermDao.name());
		queryb.append(" TT WHERE TT.");		 
		queryb.append(this.blackListFilter());
		queryb.append("; "); 
		
		long nbLines = 0;
		try{
			ResultSet couplesSet = this.executeSQLQuery(queryb.toString());
			nbLines = this.writeFile(file, couplesSet);
			couplesSet.close();
			this.closeTableGenericStatement();
		}
		catch(Exception e){
			logger.error("** PROBLEM ** Cannot write complete dictionary file " + file.getName(), e);
		}
		return nbLines;
	} 
	
	private long writeFile(File file, ResultSet couplesSet) throws IOException, SQLException {
		long nbLines = 0;
		FileWriter fstream = new FileWriter(file);
		BufferedWriter out = new BufferedWriter(fstream);
		StringBuffer lineb = new StringBuffer();
		while(couplesSet.next()){
			String column1 = couplesSet.getString(1);
			String column2 = couplesSet.getString(2);
			lineb.append(column1);
			lineb.append("\t");
			lineb.append(column2.replaceAll(NEW_LINE_REGEX, BLANK_SPACE));
			out.write(lineb.toString());
			lineb.delete(0, lineb.length());
			//out.newLine();
			// Have to be in Unix format for the mgrep tool
			out.write("\n");
			nbLines++;
		}
		out.close();
		fstream.close();
		return nbLines;
	}
		
}

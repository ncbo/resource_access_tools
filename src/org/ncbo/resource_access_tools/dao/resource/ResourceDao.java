package org.ncbo.resource_access_tools.dao.resource;

import java.net.MalformedURLException;
import java.net.URL;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

import org.ncbo.resource_access_tools.dao.AbstractObrDao;
import org.ncbo.resource_access_tools.populate.Resource;
import org.ncbo.resource_access_tools.populate.Structure;
import org.ncbo.resource_access_tools.util.MessageUtils;

import com.mysql.jdbc.exceptions.MySQLIntegrityConstraintViolationException;
import com.mysql.jdbc.exceptions.MySQLNonTransientConnectionException;

/**
 * This class is a representation for the the OBR_RT table. The table contains 
 * the following columns:
 * 
 * <ul>
 * <li> id 				 		SMALLINT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY
 * <li> name 	 		VARCHAR(150) NOT NULL
 * <li> resource_id 		  		VARCHAR(50) NOT NULL UNIQUE
 * <li> structure 		TEXT,
 * <li> main_context 			VARCHAR(50)
 * <li> url 			VARCHAR(255)
 * <li> element_url 		VARCHAR(255)
 * <li> description 	TEXT
 * <li> logo 			VARCHAR(255)
 * <li> dictionary_id            SMALLINT UNSIGNED
 * <li> total_element BIGINT
 * <li> last_update_date DATETIME
 * <li> workflow_completed_date DATETIME
 * </ul>
 *  
 * @author kyadav
 * @version OBR_v0.2		
 * @created 09-Sep-2009
 *
 */
public class ResourceDao extends AbstractObrDao {

	// Table suffix string
	private static final String TABLE_SUFFIX = MessageUtils.getMessage("obr.resource.table.suffix");

	// Prepared statement for adding new entry.
	private static PreparedStatement addEntryStatement;
	// Prepared statement for updating entry
	private static PreparedStatement updateEntryStatement;
	// Prepared statement for getting all resource entries 
	private static PreparedStatement getAllResourcesStatement;
	// Prepared statement for finding entry with corresponding ressourceID is present or not.
	private static PreparedStatement hasEntryStatement;
	// Prepared statement for finding dictionary id with corresponding ressourceID is present or not.
	private static PreparedStatement getDictioanryIDStatement;
	// Prepared statement for removing entry for specific resource.
	private static PreparedStatement resetDictionaryStatement;
	// Prepared statement for removing entry for specific resource.
	private static PreparedStatement updateNumberOfElementAndDateStatement;
	
	/**
	 * Default constructor 
	 */
	private ResourceDao() {
		super(EMPTY_STRING, TABLE_SUFFIX);
	}
	
	public static String name(){		
		return OBR_PREFIX + TABLE_SUFFIX;
	}

	@Override
	protected String creationQuery(){
		return "CREATE TABLE " + getTableSQLName() +" (" +
					"id SMALLINT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY, " +
					"name VARCHAR(150) NOT NULL, " +
					"resource_id VARCHAR(50) NOT NULL UNIQUE, " +
					"structure TEXT, " +
					"main_context VARCHAR(50), " +
					"url VARCHAR(255), " +
					"element_url VARCHAR(255), " +
					"description TEXT, " +
					"logo VARCHAR(255), " +	
					"dictionary_id SMALLINT UNSIGNED, "+ 
					"total_element BIGINT, " +
					"last_update_date TIMESTAMP NULL DEFAULT NULL, " +
					"workflow_completed_date TIMESTAMP NULL DEFAULT NULL, " +
					"INDEX X_" + this.getTableSQLName() +"_dictionary_id (dictionary_id) " +					 
				    ") ENGINE=MyISAM DEFAULT CHARSET=latin1;";
	}

	@Override
	protected void openPreparedStatements() {
		super.openPreparedStatements();
		this.openAddEntryStatement();
		this.openUpdateEntryStatement();
		this.openGetAllResourcesStatement();
		this.openHasEntryStatement();
		this.openGetDictioanryIDStatement();
		this.openResetDictionaryStatement();
		this.openUpdateNumberOfElementAndDateStatement();
	}

	@Override
	protected void closePreparedStatements() throws SQLException {
		super.closePreparedStatements();
		addEntryStatement.close();
		getAllResourcesStatement.close();
		updateEntryStatement.close();
		hasEntryStatement.close();
	}

	private static class ResourceDaoHolder {
		private final static ResourceDao RESOURCE_DAO_INSTANCE = new ResourceDao();
	}

	/**
	 * Returns a ResourceDao object by creating one if a singleton not already exists.
	 */
	public static ResourceDao getInstance(){
		return ResourceDaoHolder.RESOURCE_DAO_INSTANCE;
	}

	/****************************************** FUNCTIONS ON THE TABLE ***************************/ 
	
	@Override
	protected void openAddEntryStatement(){
		StringBuffer queryb = new StringBuffer();
		queryb.append("INSERT INTO ");
		queryb.append(this.getTableSQLName());
		queryb.append(" (name, resource_id, structure, main_context, url, element_url, description, logo ) ");
		queryb.append(" VALUES ");
		queryb.append(" (?, ?, ?, ?, ?, ?, ?, ?)");
		addEntryStatement = this.prepareSQLStatement(queryb.toString());
	}
	
	
	protected void openUpdateEntryStatement(){
		StringBuffer queryb = new StringBuffer();
		queryb.append("UPDATE ");
		queryb.append(this.getTableSQLName());
		queryb.append(" SET name= ?, structure= ?, main_context= ?, url= ?, element_url= ?, description= ?, logo= ? ");
		queryb.append(" WHERE ");
		queryb.append("resource_id= ?");
		updateEntryStatement = this.prepareSQLStatement(queryb.toString());
	}
	
	
	protected void openHasEntryStatement(){
		StringBuffer queryb = new StringBuffer();
		queryb.append("SELECT id FROM ");
		queryb.append(this.getTableSQLName());
		queryb.append(" WHERE resource_id = ?");		 
		hasEntryStatement = this.prepareSQLStatement(queryb.toString());
	}

	/**
	 * Add an new entry in corresponding SQL table.
	 * @return True if the entry was added to the SQL table, false if a problem occurred during insertion.
	 */
	public boolean addEntry(Resource resource){
		boolean inserted = false;
		try {			
			addEntryStatement.setString(1, resource.getResourceName());
			addEntryStatement.setString(2, resource.getResourceId());
			addEntryStatement.setString(3, resource.getResourceStructure().toXMLString());
			addEntryStatement.setString(4, resource.getMainContext());
			
			String resourceURL= null;
			if(resource.getResourceURL()!= null){
				resourceURL=  resource.getResourceURL().toString();
			}
			addEntryStatement.setString(5, resourceURL);
			addEntryStatement.setString(6, resource.getResourceElementURL());
			addEntryStatement.setString(7, resource.getResourceDescription());
			
			String logoURL= null;
			if(resource.getResourceLogo()!= null){
				logoURL =  resource.getResourceLogo().toString();
			}
			addEntryStatement.setString(8, logoURL);		
 
			this.executeSQLUpdate(addEntryStatement);
			inserted = true;
		}
		catch (MySQLNonTransientConnectionException e) {
			this.openAddEntryStatement();
			return this.addEntry(resource);
		}
		catch (MySQLIntegrityConstraintViolationException e){
			//logger.error("Table " + this.getTableSQLName() + " already contains an entry for the concept: " + entry.getLocalConceptID() +".");
		}
		catch (SQLException e) {			 
			logger.error("** PROBLEM ** Cannot add an entry on table " + this.getTableSQLName(), e);
			logger.error(resource.toString());
		}
		return inserted;	
	}
	
	/**
	 * This method add new entry for resource or update it if exists previously. 
	 * 
	 * @param resource
	 * @return True if the entry was added or updated to the SQL table, false if a problem occurred during insertion.
	 */
	public boolean addEntryOrUpdate(Resource resource){		
		// if entry already present then update the entry.		
		if(hasResourceEntry(resource.getResourceId())){
			return this.updateEntry(resource);
		} 
		
		return this.addEntry(resource);		 
	}	
	
	/**
	 * This method ensures whether resource entry with given resourceID is present or not.
	 * 
	 * @param resourceID
	 * @return True if the entry is present in SQL table.
	 */
	public boolean hasResourceEntry(String resourceID){ 
		boolean hasEntry = false;
		try {
			hasEntryStatement.setString(1, resourceID); 			 
			ResultSet rSet = this.executeSQLQuery(hasEntryStatement);
			
			if(rSet.next()){
				hasEntry = true;
			}		
			
			rSet.close();
		}
		catch (MySQLNonTransientConnectionException e) {
			this.openHasEntryStatement();
			return this.hasResourceEntry(resourceID);
		} 
		catch (SQLException e) {			 
			logger.error("** PROBLEM ** Cannot get resource entry from " + this.getTableSQLName(), e);			 
		}
		 
		return hasEntry;
	}
	
	/**
	 * Add an new entry in corresponding SQL table.
	 * @return True if the entry was updated to the SQL table, false if a problem occurred during insertion.
	 */
	public boolean updateEntry(Resource resource){
		boolean updated = false;
		try {
			updateEntryStatement.setString(1, resource.getResourceName());			
			updateEntryStatement.setString(2, resource.getResourceStructure().toXMLString());
			updateEntryStatement.setString(3, resource.getMainContext());
			String resourceURL= null;
			if(resource.getResourceURL()!= null){
				resourceURL=  resource.getResourceURL().toString();
			}
			updateEntryStatement.setString(4, resourceURL);
			updateEntryStatement.setString(5, resource.getResourceElementURL());
			updateEntryStatement.setString(6, resource.getResourceDescription());
			String logoURL= null;
			if(resource.getResourceLogo()!= null){
				logoURL =  resource.getResourceLogo().toString();
			}
			updateEntryStatement.setString(7, logoURL);
			updateEntryStatement.setString(8, resource.getResourceId());
 
			this.executeSQLUpdate(updateEntryStatement);
			
			logger.info("Resource entry updated.");
			updated = true;
		}
		catch (MySQLNonTransientConnectionException e) {
			this.openUpdateEntryStatement();
			return this.updateEntry(resource);
		}		 
		catch (SQLException e) {			 
			logger.error("** PROBLEM ** Cannot update an entry on table " + this.getTableSQLName(), e);
			logger.error(resource.toString());
		}
		return updated;	
	}
	
	private void openGetAllResourcesStatement(){
		StringBuffer queryb = new StringBuffer();
		queryb.append("SELECT name, resource_id, structure, main_context, url, element_url, description, logo FROM ");
		queryb.append(this.getTableSQLName());		 
		queryb.append(";");
		getAllResourcesStatement = this.prepareSQLStatement(queryb.toString());
	} 
	
	/**
	 * Update table with given dictionary id and also update workflow_completed_date with current date.
	 * 
	 * @param resource 
	 * @param dictionaryID 
	 * 
	 * @return True if the entry was updated to the SQL table.
	 */
	public boolean updateDictionaryAndWorkflowDate(Resource resource, int dictionaryID){
		boolean updated = false;
		try {
			StringBuffer queryb = new StringBuffer();
			queryb.append("UPDATE ");
			queryb.append(this.getTableSQLName());
			queryb.append(" SET dictionary_id=");
			queryb.append(dictionaryID);
			queryb.append(", workflow_completed_date= NOW() WHERE ");
			queryb.append("resource_id= '");
			queryb.append(resource.getResourceId());
			queryb.append("';");
			
			this.executeSQLUpdate(queryb.toString());			
			logger.info("Resource entry updated with latest dictionary ID.");
			updated = true;
		}
		catch (MySQLNonTransientConnectionException e) {			 
			return this.updateDictionaryAndWorkflowDate(resource, dictionaryID);
		}		 
		catch (SQLException e) {			 
			logger.error("** PROBLEM ** Cannot update dictionary ID on table " + this.getTableSQLName(), e);
			logger.error(resource.toString());
		}
		return updated;	
	}
	
	 
	protected void openResetDictionaryStatement(){
		StringBuffer queryb = new StringBuffer();
		queryb.append("UPDATE ");
		queryb.append(this.getTableSQLName());
		queryb.append(" SET dictionary_id= NULL WHERE ");
		queryb.append("resource_id= ? ;");
		resetDictionaryStatement = this.prepareSQLStatement(queryb.toString());
	}
	
	public boolean resetDictionary(String resourceID){
		boolean updated = false;
		try{
			resetDictionaryStatement.setString(1, resourceID);
			this.executeSQLUpdate(resetDictionaryStatement);
			updated = true;
		}		
		catch (MySQLNonTransientConnectionException e) {
			this.openResetDictionaryStatement();
			return this.resetDictionary(resourceID);
		}
		catch (SQLException e) {
			logger.error("** PROBLEM ** Cannot update dictionary ID on table " + this.getTableSQLName(), e);
		}
		return updated;
	}
	
	protected void openUpdateNumberOfElementAndDateStatement(){
		StringBuffer queryb = new StringBuffer();
		queryb.append("UPDATE ");
		queryb.append(this.getTableSQLName());
		queryb.append(" SET total_element= ?, last_update_date= NOW() WHERE ");
		queryb.append("resource_id= ? ;");
		updateNumberOfElementAndDateStatement = this.prepareSQLStatement(queryb.toString());
	}
	
	/**
	 * This method update number of entries present in element table for given resource and
	 * also update last_update_date column with current date.
	 * 
	 * @param resourceID 
	 * @param numberOfElements 
	 * @return boolean {@code true} if updated successfully.
	 */
	public boolean updateNumberOfElementAndDate(String resourceID, int numberOfElements){
		boolean updated = false;
		try{
			updateNumberOfElementAndDateStatement.setInt(1, numberOfElements);
			updateNumberOfElementAndDateStatement.setString(2, resourceID);
			this.executeSQLUpdate(updateNumberOfElementAndDateStatement);
			updated = true;
		}		
		catch (MySQLNonTransientConnectionException e) {
			this.openResetDictionaryStatement();
			return this.resetDictionary(resourceID);
		}
		catch (SQLException e) {
			logger.error("** PROBLEM ** Cannot update dictionary ID on table " + this.getTableSQLName(), e);
		}
		return updated;
	}
	
	/**
	 * This method get list of all the resources from Resource Table (OBR_RT) 
	 * 
	 * @return list of Resource
	 */
	public ArrayList<Resource> getAllResources(){
		
		ArrayList<Resource> resources= new ArrayList<Resource>(1);
		Resource resource;
		try {
			ResultSet rSet = this.executeSQLQuery(getAllResourcesStatement);
			while(rSet.next()){ 				
				try {
					// Creating resource object from Resource.
					resource = new Resource(rSet.getString(1), rSet.getString(2),						
								Structure.createStructureFromXML(rSet.getString(3)), rSet.getString(4),
								new URL(rSet.getString(5)),rSet.getString(6),  rSet.getString(7),new URL(rSet.getString(8))
							 
							);
					resources.add(resource); 
				} catch (MalformedURLException e) {
					 logger.error("Problem in getting resource :" +rSet.getString(1));
				} 
			}
			rSet.close();			 
		}
		catch (MySQLNonTransientConnectionException e) {
			this.openGetAllResourcesStatement();
			return this.getAllResources();
		}
		catch (SQLException e) {
			logger.error("** PROBLEM ** Cannot get resource. ", e);
		} 
		
		return resources;
	}
	
	
	protected void openGetDictioanryIDStatement(){
		StringBuffer queryb = new StringBuffer();
		queryb.append("SELECT dictionary_id FROM ");
		queryb.append(this.getTableSQLName());
		queryb.append(" WHERE resource_id = ?");		 
		getDictioanryIDStatement = this.prepareSQLStatement(queryb.toString());
	}
	
	/**
	 * 
	 * Get the dictionary id processed by given resource.
	 * 
	 * @param resourceID
	 * @return int - dictionary id 
	 */
	public int getDictionaryId(String resourceID){
		int dictionaryID = -1;
		try {
			getDictioanryIDStatement.setString(1, resourceID); 			 
			ResultSet rSet = this.executeSQLQuery(getDictioanryIDStatement);
			
			if(rSet.next()){
				dictionaryID = rSet.getInt(1);
			}
			 
			rSet.close();
		}
		catch (MySQLNonTransientConnectionException e) {
			this.openHasEntryStatement();
			return this.getDictionaryId(resourceID);
		} 
		catch (SQLException e) {			 
			logger.error("** PROBLEM ** Cannot get resource entry from " + this.getTableSQLName(), e);			 
		}
		 
		return dictionaryID;
	}
	
	
	/**
	 * This method gives id(primary key) from SQL table for given resourceID
	 * 
	 * @param resourceID
	 * @return int - id .
	 */
	public int getResourceIdKey(String resourceID){ 
		int id= -1;
		try {
			hasEntryStatement.setString(1, resourceID); 			 
			ResultSet rSet = this.executeSQLQuery(hasEntryStatement);
			
			if(rSet.next()){
				 id = rSet.getInt(1);
			}
			 
			rSet.close();
		}
		catch (MySQLNonTransientConnectionException e) {
			this.openHasEntryStatement();
			return this.getResourceIdKey(resourceID);
		} 
		catch (SQLException e) {			 
			logger.error("** PROBLEM ** Cannot get resource id from " + this.getTableSQLName(), e);			 
		}
		 
		return id;
	}

	 
}

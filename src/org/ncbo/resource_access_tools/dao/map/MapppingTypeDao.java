package org.ncbo.resource_access_tools.dao.map;

import java.sql.SQLException;

import org.ncbo.resource_access_tools.dao.obs.AbstractObsDao;
import org.ncbo.resource_access_tools.util.MessageUtils;
/**
 * This class is a representation for the OBS(slave) DB obs_map table. The table contains 
 * the following columns:
 * <ul> 
 * <li>id INT(11) NOT NULL PRIMARY KEY 
   <li>mapping_type VARCHAR(20) NOT NULL  UNIQUE KEY
 * </ul>
 * 
 */
public class MapppingTypeDao extends AbstractObsDao{
	
	private static final String TABLE_SUFFIX = MessageUtils.getMessage("obs.mappig_type.table.suffix");
  
	private MapppingTypeDao() {
		super(TABLE_SUFFIX);

	}
	
	private static class MappingTypeDaoHolder {
		private final static MapppingTypeDao MAP_DAO_INSTANCE = new MapppingTypeDao();
	}

	/**
	 * Returns a ConceptTable object by creating one if a singleton not already exists.
	 */
	public static MapppingTypeDao getInstance(){
		return MappingTypeDaoHolder.MAP_DAO_INSTANCE;
	}
	
	public static String name(String resourceID){		
		return OBS_PREFIX + TABLE_SUFFIX;
	}

	@Override
	protected void openPreparedStatements() {
		super.openPreparedStatements();		 	 
	}
	@Override
	protected void closePreparedStatements() throws SQLException {
		super.closePreparedStatements();
		 
	}
	 
	@Override
	protected String creationQuery() {
		return "CREATE TABLE " + this.getTableSQLName() +" (" +
		"id INT(11) NOT NULL AUTO_INCREMENT PRIMARY KEY, " +
		"mapping_type VARCHAR(30) NOT NULL, " +
		"UNIQUE (mapping_type) "+
		") ENGINE=MyISAM DEFAULT CHARSET=latin1; ";
	}

	@Override
	protected void openAddEntryStatement() throws SQLException {
		// TODO Auto-generated method stub
		
	} 
}

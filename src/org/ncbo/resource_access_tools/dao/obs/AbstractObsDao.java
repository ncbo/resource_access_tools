package org.ncbo.resource_access_tools.dao.obs;

import org.ncbo.resource_access_tools.dao.AbstractObrDao;
 
public abstract class AbstractObsDao extends AbstractObrDao {  
	  	
	public AbstractObsDao(String suffix) {
		 super(OBS_PREFIX + suffix);
	}  
	
	 	
}
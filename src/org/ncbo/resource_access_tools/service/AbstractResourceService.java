package org.ncbo.resource_access_tools.service;

import org.apache.log4j.Logger;
import org.ncbo.resource_access_tools.dao.element.ElementDao;
import org.ncbo.resource_access_tools.resource.ResourceAccessTool;
import org.ncbo.resource_access_tools.util.helper.StringHelper;

public abstract class AbstractResourceService implements StringHelper{	
	
	// Logger for AbstractResourceService 
	protected static Logger logger = Logger.getLogger(AbstractResourceService.class);
	
	protected static ResourceAccessTool resourceAccessTool;	
	
	protected static ElementDao elementTableDao;
	
	
	  
	public AbstractResourceService(ResourceAccessTool resourceAccessTool) {
		super();
		
		if(AbstractResourceService.resourceAccessTool== null 
				|| AbstractResourceService.resourceAccessTool.getToolResource().getResourceId()!=resourceAccessTool.getToolResource().getResourceId()){
			 AbstractResourceService.resourceAccessTool = resourceAccessTool;
				
			// Creating Element Table Dao for given resource access tool
			 elementTableDao= new ElementDao(resourceAccessTool.getToolResource().getResourceId() 
					, resourceAccessTool.getToolResource().getResourceStructure()) ;
					 
			 		
		}
		
		
	}
	
	
	

}

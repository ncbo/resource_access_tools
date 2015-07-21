package org.ncbo.resource_access_tools.service;

import org.apache.log4j.Logger;
import org.ncbo.resource_access_tools.dao.DaoFactory;
import org.ncbo.resource_access_tools.dao.aggregation.AggregationDao;
import org.ncbo.resource_access_tools.dao.aggregation.ConceptFrequencyDao;
import org.ncbo.resource_access_tools.dao.annotation.DirectAnnotationDao;
import org.ncbo.resource_access_tools.dao.annotation.expanded.IsaExpandedAnnotationDao;
import org.ncbo.resource_access_tools.dao.annotation.expanded.MapExpandedAnnotationDao;
import org.ncbo.resource_access_tools.dao.element.ElementDao;
import org.ncbo.resource_access_tools.resource.ResourceAccessTool;
import org.ncbo.resource_access_tools.util.helper.StringHelper;

public abstract class AbstractResourceService implements DaoFactory, StringHelper{	
	
	// Logger for AbstractResourceService 
	protected static Logger logger = Logger.getLogger(AbstractResourceService.class);
	
	protected static ResourceAccessTool resourceAccessTool;	
	
	protected static ElementDao elementTableDao;
	protected static DirectAnnotationDao directAnnotationTableDao;
	protected static IsaExpandedAnnotationDao isaExpandedAnnotationTableDao;
	protected static MapExpandedAnnotationDao mapExpandedAnnotationTableDao;
	protected static AggregationDao aggregationTableDao;
	protected static ConceptFrequencyDao conceptFrequencyDao;
	
	  
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

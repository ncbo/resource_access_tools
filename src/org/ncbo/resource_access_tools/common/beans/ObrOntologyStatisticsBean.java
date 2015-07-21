package org.ncbo.resource_access_tools.common.beans;


/**
 * Ontology statistics beans wraps statistics for particular resource.
 * 
 * @author kyadav
 *
 */
public class ObrOntologyStatisticsBean {
   
	private String resourceId;
	
	private ObrStatisticsBean statistics;
	
	 
	public ObrOntologyStatisticsBean() {
		 
	}


	/**
	 * @return the resourceId
	 */
	public String getResourceId() {
		return resourceId;
	}


	/**
	 * @param resourceId the resourceId to set
	 */
	public void setResourceId(String resourceId) {
		this.resourceId = resourceId;
	}


	/**
	 * @return the statistics
	 */
	public ObrStatisticsBean getStatistics() {
		return statistics;
	}


	/**
	 * @param statistics the statistics to set
	 */
	public void setStatistics(ObrStatisticsBean statistics) {
		this.statistics = statistics;
	}


	  

}

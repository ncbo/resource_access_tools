
package org.ncbo.resource_access_tools.populate;

import java.net.URL;
import java.util.Date;


/**
 * This class is a representation for an online resource used in OBR.
 * An online resource is viewed as a set of elements.<p>
 * This class contains a name, a resourceId (used for DB tables names) a Structure (with null values) that
 * defines the structure  the resource elements have. A mainContext that specify the best context descriptor
 * to describe the resource element. 
 * 
 * It also has a description of the corresponding 
 * online resource, an URL to link to the online resource and a URL to link to the online resource logo.
 * It also contains the basis URL (String) to use to generate a complete URL with a localElementId.
 *  
 * @author Clement Jonquet
 * @version OBR v_0.2
 * @created 02-Oct-2007 11:47:48 AM
 */
public class Resource {

	private String resourceName;
	private String resourceId;
	private Structure resourceStructure;
	private String mainContext;
	private URL resourceURL;
	private String resourceElementURL;
	private String resourceDescription;
	private URL resourceLogo;
	private Date lastUpdateDate;
	private Date workflowCompletedDate;
	private long totalElements;
	
			
	/**
	 * Constructs a new Resource with a given name, resourceId and ResourceAccessTool.
	 */
	public Resource(String resourceName, String resourceId, Structure resourceStructure, String mainContext) {
		super();
		this.resourceName = resourceName;
		this.resourceId = resourceId;
		this.resourceStructure = resourceStructure;
		this.mainContext = mainContext;
		this.resourceElementURL = null;
		this.resourceURL = null;
		this.resourceDescription = "NoDescription";
		this.resourceLogo = null;
	}
	
	
	public Resource(String resourceName, String resourceId,
			Structure resourceStructure, String mainContext, URL resourceURL,
			String resourceElementURL, String resourceDescription,
			URL resourceLogo) {
		super();
		this.resourceName = resourceName;
		this.resourceId = resourceId;
		this.resourceStructure = resourceStructure;
		this.mainContext = mainContext;
		this.resourceURL = resourceURL;
		this.resourceElementURL = resourceElementURL;
		this.resourceDescription = resourceDescription;
		this.resourceLogo = resourceLogo;
	}
	
	public Resource(String resourceName, String resourceId,
			Structure resourceStructure, String mainContext, URL resourceURL,
			String resourceElementURL, String resourceDescription,
			URL resourceLogo, long totalElements, Date lastUpdateDate, Date workflowCompletedDate) {
		super();
		this.resourceName = resourceName;
		this.resourceId = resourceId;
		this.resourceStructure = resourceStructure;
		this.mainContext = mainContext;
		this.resourceURL = resourceURL;
		this.resourceElementURL = resourceElementURL;
		this.resourceDescription = resourceDescription;
		this.resourceLogo = resourceLogo;
		this.totalElements = totalElements;
		this.lastUpdateDate = lastUpdateDate;
		this.workflowCompletedDate= workflowCompletedDate;
	}


	public void finalize() throws Throwable { super.finalize();
	}

	public String getResourceName() {
		return resourceName;
	}
	
	public String getResourceId() {
		return resourceId;
	}

	public Structure getResourceStructure() {
		return resourceStructure;
	}
	
	public String getMainContext() {
		return mainContext;
	}

	public URL getResourceURL() {
		return resourceURL;
	}
	
	public String getResourceElementURL() {
		return resourceElementURL;
	}

	public void setResourceURL(URL resourceURL) {
		this.resourceURL = resourceURL;
	}

	public void setResourceElementURL(String resourceElementURL) {
		this.resourceElementURL = resourceElementURL;
	}
	
	public String getResourceDescription() {
		return resourceDescription;
	}
	
	public void setResourceDescription(String resourceDescription) {
		this.resourceDescription = resourceDescription;
	}
	
	public URL getResourceLogo() {
		return resourceLogo;
	}

	public void setResourceLogo(URL resourceLogo) {
		this.resourceLogo = resourceLogo;
	}

	
	public Date getLastUpdateDate() {
		return lastUpdateDate;
	} 

	public Date getWorkflowCompletedDate() {
		return workflowCompletedDate;
	}
	
	


	public long getTotalElements() {
		return totalElements;
	}


	/**
	 * Returns a String that can be printed that contains the Resource information. 
	 */
	public String toString(){
		String[] resourceString = new String[7]; 
		resourceString[0] = "----------------------------------";
		resourceString[1] = "Object Resource:";
		resourceString[2] = "URL: \t\t" + this.getResourceURL().toString();
		resourceString[3] = "Name: \t\t" + this.getResourceName();
		resourceString[4] = "Description: \t" + this.getResourceDescription().substring(0, 100) + " ...";
		resourceString[5] = "Structure: \t" + this.getResourceStructure().toString();
		resourceString[6] = "----------------------------------";
		return resourceString.toString();
	}
}
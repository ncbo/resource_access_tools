package org.ncbo.resource_access_tools.common.beans;

import java.util.HashSet;

import org.ncbo.resource_access_tools.common.utils.Utilities;

/**
* This class implements the specific parameters OBR queries.
*
* @author Clement Jonquet
* @version OBS_v1
* @created 09-Dec-2008
*/
public class ObrParametersBean {
	
	//TODO: make the ObrParametersBean a sub class of ParametersBean 
	
	private HashSet<String> localConceptIds;
	private String localElementId;
	private String resourceId;
	private String mode;
	private boolean withContext;
	private boolean elementDetails;
	private boolean counts;
	private int offset;
	private int limit;
	
	/**
	 * Constructs a new set of parameters to be used with {@link OpenBiomedicalResource}}.
	 *  
	 * @param localConceptIds 
	 * Set of concept to use to get annotations.
	 * @param localElementId
	 * Element for which to get annotations.
	 * @param resourceId
	 * Resource for which to get annotations from.
	 * @param mode
	 * Mode to use in case of several concept. UNION or INTERSECTION.
	 * @param withContext
	 * Full result bean of simplified one (context of annotations empty). 
	 * @param elementDetails
	 * Does the result bean include full Element bean or not. 
	 * @param counts
	 * To get the counts of the query instead of the results themselves.
	 * @param offset
	 * Offset of annotations start. 
	 * @param limit
	 * Number of annotations return in the offset. 
	 */
	public ObrParametersBean(HashSet<String> localConceptIds, String localElementId, String resourceId, String mode, boolean withContext, boolean elementDetails, boolean counts, int offset, int limit) {
		super();
		this.localConceptIds = localConceptIds;
		this.localElementId = localElementId;
		this.resourceId = resourceId;
		this.mode = mode;
		this.withContext = withContext;
		this.elementDetails= elementDetails;
		this.counts = counts;
		this.offset = offset;
		this.limit = limit;
	}

	public ObrParametersBean(String[] localConceptIds, String localElementId, String resourceId, String mode, boolean withContext, boolean elementDetails, boolean counts, int offset, int limit) {
		this(Utilities.arrayToHashSet(localConceptIds), localElementId, resourceId, mode, withContext, elementDetails, counts, offset, limit);
	}
	
	public HashSet<String> getLocalConceptIds() {
		return localConceptIds;
	}

	public String getLocalElementId() {
		return localElementId;
	}

	public String getResourceId() {
		return resourceId;
	}
	
	public void setResourceId(String resourceId) {
		this.resourceId = resourceId;
	}

	public String getMode() {
		return mode;
	}

	public boolean isWithContext() {
		return withContext;
	}
	
	public boolean isElementDetails() {
		return elementDetails;
	}
	
	public boolean isCounts() {
		return counts;
	} 
	
	public int getOffset() {
		return offset;
	}

	public int getLimit() {
		return limit;
	}

	/**
	 * Constructs a <code>String</code> with all attributes in name = value format.
	 * @return a <code>String</code> representation of this object.
	 */
	public String toString(){
	    final String TAB = ", ";
	    StringBuffer retValue = new StringBuffer();
	    retValue.append("parameters [ ")
	        //.append(super.toString()).append(TAB)
	        .append("localConceptIds = ").append(this.localConceptIds).append(TAB)
	        .append("localElementId = ").append(this.localElementId).append(TAB)
	        .append("resourceId = ").append(this.resourceId).append(TAB)
	        .append("mode = ").append(this.mode).append(TAB)
	        .append("withContext = ").append(this.withContext).append(TAB)
	        .append("elementDetails = ").append(this.elementDetails).append(TAB)
	        .append("counts = ").append(this.counts).append(TAB)
	        .append("offset = ").append(this.offset).append(TAB)
	        .append("limit = ").append(this.limit).append(TAB)
	        .append("]");
	    return retValue.toString();
	}

}
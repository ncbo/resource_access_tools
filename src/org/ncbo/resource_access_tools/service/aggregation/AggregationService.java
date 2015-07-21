/**
 * 
 */
package org.ncbo.resource_access_tools.service.aggregation;

import java.util.List;

import org.ncbo.resource_access_tools.enumeration.ResourceType;
import org.ncbo.resource_access_tools.populate.ObrWeight;

/**
 * @author Kuladip Yadav
 *
 */
public interface AggregationService {
	
	/**
	 * Processes the resource direct & expanded annotations to produce the index and
	 * populates the the corresponding _IT using a set weights.
	 * This function implements the step 4 of the OBR workflow.
	 * 
	 * @param weights  Used for calculating score
	 * @return The number of annotations created in the index. 
	 * 
	 */
	public long aggregation(ObrWeight weights);
	
	/**
	 *  Sort Aggregation
	 * 
	 * @param resourceType  Used for calculating score
	 * @return  Boolean  
	 * 
	 */
	public boolean sortAggregation(ResourceType resourceType);
	
	/**
	 * Method removes indexing done for given ontology versions.
	 * 
	 * @param {@code List} of localOntologyID String containing ontology version.
	 */
	public void removeAggregation(List<String> localOntologyID);

	/**
	 * Method calculates concept frequency from aggregation table 
	 *  
	 * @return The number of annotations created in the index. 
	 * 
	 */
	public long calulateConceptFrequncy();
	
	/**
	 * Method removes concept frequency calculation done for given ontology versions.
	 * 
	 * @param {@code List} of localOntologyID String containing ontology version.
	 */
	public void removeConceptFrequncy(List<String> localOntologyID);

	/**
	 *  Enable indexes for aggregation table
	 *   
	 * @param bigResource
	 * @return
	 */
	public boolean enableIndexes(boolean bigResource);
	
	/**
	 * Disable indexes for aggregation table
	 * 
	 * @return
	 */
	public boolean disableIndexes();
}

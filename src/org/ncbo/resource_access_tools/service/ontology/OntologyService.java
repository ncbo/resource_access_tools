package org.ncbo.resource_access_tools.service.ontology;

/**
 * This service interface provides different functionality related to slave ontology tables. 
 * 
 * @author Kuladip Yadav
 *
 */
public interface OntologyService {
	
	/**
	 * This method gets latest version of ontology for given virtual ontology id
	 * 
	 * @param virtualOntologyID 
	 * @return String of latest version of ontology.
	 */
	public String getLatestLocalOntologyID(String virtualOntologyID);

}

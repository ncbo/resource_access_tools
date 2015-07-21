/**
 * 
 */
package org.ncbo.resource_access_tools.service.semantic;

import java.util.List;

import org.ncbo.resource_access_tools.util.MessageUtils;

/**
 * @author Kuladip Yadav
 *
 */
public interface SemanticExpansionService {
	
	/** For semantic expansion all level*/
	public static final int LEVEL_ALL = -1; 

	/** semantic expansion level for big resources */
	public static final int MAX_LEVEL_FOR_BIG_RESOURCE = Integer.parseInt(MessageUtils.getMessage("obr.expanded.annotation.max.level"));
	
	/** 
	 * Processes the resource direct annotations to produce expanded annotations and
	 * populates the the corresponding _EAT.
	 * This function implements the step 3 of the OBR workflow.
	 * The 3 booleans corresponds to the semantic expansion component to use.
	 * 
	 * @param isaClosureExpansion {@code boolean} for is a closure expansion
	 * @param mappingExpansion    {@code boolean} for mapping expansion
	 * @param distanceExpansion   {@code boolean} for mapping expansion
	 * @return                    the number of direct annotations created. 
	 */
	public long semanticExpansion(boolean isaClosureExpansion, boolean mappingExpansion, boolean distanceExpansion);

	/**
	 * Method removes expanded annotations for given ontology versions.Entries are remove from 
	 * is a parent relation and mapping relation.
	 * 
	 * @param {@code List} of localOntologyIDs String containing ontology versions.
	 */
	public void removeExpandedAnnotations(List<String> localOntologyIDs);
 
	public void createIndexForExpandedAnnotationTables();
}

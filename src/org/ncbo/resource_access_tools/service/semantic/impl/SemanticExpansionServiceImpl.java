package org.ncbo.resource_access_tools.service.semantic.impl;

import java.util.List;

import obs.common.utils.ExecutionTimer;

import org.ncbo.resource_access_tools.resource.ResourceAccessTool;
import org.ncbo.resource_access_tools.service.AbstractResourceService;
import org.ncbo.resource_access_tools.service.semantic.SemanticExpansionService;

public class SemanticExpansionServiceImpl extends AbstractResourceService implements SemanticExpansionService{

	public SemanticExpansionServiceImpl(ResourceAccessTool resourceAccessTool) {
		super(resourceAccessTool);
		// TODO Auto-generated constructor stub
	}	 

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
	public long semanticExpansion(boolean isaClosureExpansion, boolean mappingExpansion, boolean distanceExpansion){
		long nbAnnotation = 0;
		ExecutionTimer timer1 = new ExecutionTimer();
		timer1.start();
		logger.info("*** Executing  annotation expansion process... ");
		ExecutionTimer timer = new ExecutionTimer();
		// isaClosure expansion
		if(isaClosureExpansion){
			timer.start();
			logger.info("\t** Executing isa transitive closure expansion... ");
			long isaAnnotation = isaExpandedAnnotationTableDao.isaClosureExpansion(directAnnotationTableDao);
			logger.info("\t\t" +isaAnnotation);
			nbAnnotation += isaAnnotation;
			timer.end();
			logger.info("\t## Isa transitive closure expansion  processed in: " + timer.millisecondsToTimeString(timer.duration()));
			timer.reset();
		}
		// mapping expansion
		if(mappingExpansion){
			timer.start();
			logger.info("\t** Executing mapping expansion... ");
			long mappingAnnotation = mapExpandedAnnotationTableDao.mappingExpansion(directAnnotationTableDao);
			logger.info("\t\t" + mappingAnnotation);
			nbAnnotation += mappingAnnotation;
			timer.end();
			logger.info("\t## Mapping expansion processed in: " + timer.millisecondsToTimeString(timer.duration()));
			timer.reset();
		}
		// distance expansion
		if(distanceExpansion){
			timer.start();
			logger.info("\t** Distance semantic expansion not implemeted yet.");
			timer.end();
			logger.info("\t## Distance expansion processed in: " + timer.millisecondsToTimeString(timer.duration()));
			timer.reset();
		}
		
		timer1.end();
		logger.info("### Annotation expansion processed in: " + timer1.millisecondsToTimeString(timer1.duration()));
		
		return nbAnnotation;
	}
	
	/**
	 * Method removes expanded annotations for given ontology versions.Entries are remove from 
	 * is a parent relation and mapping relation.
	 * 
	 * <p>For big resources, it remove local ontology id one by one
	 * <p>For other resources remove all local ontology ids
	 * 
	 * @param {@code List} of localOntologyIDs String containing ontology versions.
	 */
	public void removeExpandedAnnotations(List<String> localOntologyIDs) {
		
//		if(resourceAccessTool.getResourceType()!= ResourceType.BIG){
			isaExpandedAnnotationTableDao.deleteEntriesFromOntologies(localOntologyIDs); 
			mapExpandedAnnotationTableDao.deleteEntriesFromOntologies(localOntologyIDs); 
//		 }else{
//			 for (String localOntologyID : localOntologyIDs) {
//				 isaExpandedAnnotationTableDao.deleteEntriesFromOntology(localOntologyID); 
//				 mapExpandedAnnotationTableDao.deleteEntriesFromOntology(localOntologyID); 
//			}
//			 
//		 }	
	}

	/*
	 * (non-Javadoc)
	 * @see org.ncbo.stanford.obr.service.semantic.SemanticExpansionService#createIndexForExpandedAnnotationTable()
	 */
	public void createIndexForExpandedAnnotationTables() {
		if(!isaExpandedAnnotationTableDao.indexesExist()){
			isaExpandedAnnotationTableDao.createIndexes();	 
			logger.info("\tIndexes created for table " + isaExpandedAnnotationTableDao.getTableSQLName());
		}else{
			logger.info("\tIndexes already present in table " + isaExpandedAnnotationTableDao.getTableSQLName());
		}
		
		if(!mapExpandedAnnotationTableDao.indexesExist()){
			mapExpandedAnnotationTableDao.createIndexes();	 
			logger.info("\tIndexes created for table " + mapExpandedAnnotationTableDao.getTableSQLName());
		} else{
			logger.info("\tIndexes already present in table " + mapExpandedAnnotationTableDao.getTableSQLName());
		}
		
	}

}

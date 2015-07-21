package org.ncbo.resource_access_tools.service.annotation.impl;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;

import obs.common.utils.ExecutionTimer;

import org.apache.log4j.Logger;
import org.ncbo.resource_access_tools.common.beans.DictionaryBean;
import org.ncbo.resource_access_tools.dao.dictionary.DictionaryDao;
import org.ncbo.resource_access_tools.populate.Structure;
import org.ncbo.resource_access_tools.resource.ResourceAccessTool;
import org.ncbo.resource_access_tools.service.AbstractResourceService;
import org.ncbo.resource_access_tools.service.annotation.AnnotationService;
import org.ncbo.resource_access_tools.util.FileResourceParameters;
import org.ncbo.resource_access_tools.util.mgrep.ConceptRecognitionTools;

public class AnnotationServiceImpl extends AbstractResourceService implements
		AnnotationService {

	// Logger for AnnotationServiceImpl
	protected static Logger logger = Logger
			.getLogger(AnnotationServiceImpl.class);

	public AnnotationServiceImpl(ResourceAccessTool resourceAccessTool) {
		super(resourceAccessTool);
	}
	 
	/** 
	 * Processes the resource with Mgrep and populates the the corresponding _DAT.
	 * The given boolean specifies if the complete dictionary must be used, or not (only the delta dictionary).
	 * The annotation done with termName that are in the specified stopword list are removed from _DAT.
	 * This function implements the step 2 of the OBR workflow.
	 *  
	 * @param withCompleteDictionary if true uses complete dictionary for annotation otherwise uses delta dictionary
	 * @param dictionary Latest dictionary bean
	 * @param stopwords {@code Set} of string used as stopwords
	 * @return {@code int} the number of direct annotations created. 
	 */
	public long resourceAnnotation(boolean withCompleteDictionary, DictionaryBean dictionary, 
			HashSet<String> stopwords) {
		long nbAnnotation =0;		 
		ExecutionTimer timer = new ExecutionTimer();
		timer.start();
		logger.info("*** Executing  Direct Annotation process... ");
		// processes direct mgrep annotations
		
		if(elementTableDao.numberOfElementsForMgrepAnnotation(dictionary.getDictionaryId())> 0){
			nbAnnotation = this.conceptRecognitionWithMgrep(dictionary,
			 		withCompleteDictionary, stopwords);
		} else{
			logger.info("\tNo element present in "+ elementTableDao.getTableSQLName()+ " for MGRAP annotation.");
		}
		// processes direct reported annotations
		nbAnnotation += this.reportExistingAnnotations(dictionary);

		// updates the dictionary column in _ET
		logger.info("\tUpdating the dictionary field in ElementTable...");		  
		// Update dictionary id for element table.		 
		elementTableDao.updateDictionary(dictionary.getDictionaryId());	
		
		timer.end();
		logger.info("### Direct annotation processed in: " + timer.millisecondsToTimeString(timer.duration()));
			
		return nbAnnotation;
	}

	/**
	 * Applies Mgrep on the corresponding resource. Only the elements in _ET
	 * with a dictionaryID < to the latest one are selected (or the one with
	 * null); Returns the number of annotations added to _DAT.
	 */
	private long conceptRecognitionWithMgrep(DictionaryBean dictionary,
			boolean withCompleteDictionary, HashSet<String> stopwords) {
		long nbDirectAnnotation = 0;
		ExecutionTimer timer1 = new ExecutionTimer();
		ExecutionTimer timer = new ExecutionTimer();

		logger.info("\t** Concept recognition with Mgrep:");
		timer1.start();
		// Checks if the dictionary file exists
		File dictionaryFile;
		try {
			if (withCompleteDictionary) {
				dictionaryFile = new File(DictionaryDao
						.completeDictionaryFileName(dictionary));
			} else {
				logger.info("Else block 1:: "+dictionary.getDictionaryName());
				dictionaryFile = new File(DictionaryDao
						.dictionaryFileName(dictionary));
			}
			if (dictionaryFile.createNewFile()) {
				logger.info("\t\tRe-creation of the dictionaryFile...");			 
				
				if (withCompleteDictionary) {
					dictionaryDao.writeDictionaryFile(dictionaryFile);
				} else {
					dictionaryDao.writeDictionaryFile(dictionaryFile, dictionary
							.getDictionaryId());
				}
			}
		} catch (IOException e) {
			dictionaryFile = null;
			logger
					.error(
							"\t\t** PROBLEM ** Cannot create the dictionaryFile. null returned.",
							e);
		}

		// Writes the resource file with the elements not processed with the
		// latest dictionary
		timer.start();
		File resourceFile = this.writeMgrepResourceFile(dictionary
				.getDictionaryId());
		timer.end();
		logger.info("\t\tResourceFile created in: "
				+ timer.millisecondsToTimeString(timer.duration()) + "\n");

		// Calls Mgrep
		timer.reset();
		timer.start();
		File mgrepFile = this.mgrepCall(dictionaryFile, resourceFile);
		timer.end();
		logger.info("\t\tMgrep executed in: "
				+ timer.millisecondsToTimeString(timer.duration()));

		// Process the Mgrep result file
		timer.reset();
		timer.start();
		nbDirectAnnotation = this.processMgrepFile(mgrepFile, dictionary
				.getDictionaryId());
		timer.end();
		logger.info("\t\tMgrepFile processed in: "
				+ timer.millisecondsToTimeString(timer.duration()));

		// Deletes the files created for Mgrep and generated by Mgrep
		resourceFile.delete();
		mgrepFile.delete(); 
		
		timer.end();
		logger.info("\t## Total MGREP processed in: "
				+ timer.millisecondsToTimeString(timer.duration()));

		return nbDirectAnnotation;
	}

	private File mgrepCall(File dictionaryFile, File resourceFile) {
		logger.info("Call to Mgrep...");
		File mgrepFile = null;
		try {
			mgrepFile = ConceptRecognitionTools.mgrepLocal(dictionaryFile,
					resourceFile);
		} catch (IOException e) {
			logger.error("** PROBLEM ** Cannot create MgrepFile.", e);
		} catch (Exception e) {
			logger.error("** PROBLEM ** Cannot execute Mgrep.", e);
		}

		return mgrepFile;

	}

	private long processMgrepFile(File mgrepFile, int dictionaryID) {
		long nbAnnotation = -1;
		logger.info("Processing of the result file...");
		nbAnnotation = directAnnotationTableDao.loadMgrepFile(mgrepFile,
				dictionaryID);
		logger.info(nbAnnotation + " annotations done with Mgrep.");
		return nbAnnotation;
	}

	/********************************* EXPORT CONTENT FUNCTIONS *****************************************************/

	/**
	 * Returns a file that respects the Mgrep resource file requirements. This
	 * text file has 3 columns: [integer | integer | text] they are respectively
	 * [elementID | contextID | text]. The file contains only the element that
	 * have been already annotated with a previous version of the given
	 * dictionary (or never annotated).
	 */
	public File writeMgrepResourceFile(int dictionaryID) {
		logger
				.info("Exporting the resource content to a file to be annotated with Mgrep...");
		String name = FileResourceParameters.mgrepInputFolder()
				+ ResourceAccessTool.RESOURCE_NAME_PREFIX
				+ resourceAccessTool.getToolResource().getResourceId() + "_V"
				+ dictionaryID + "_MGREP.txt";
		File mgrepResourceFile = new File(name);
		try {
			mgrepResourceFile.createNewFile();	 
			
			elementTableDao.writeNonAnnotatedElements(mgrepResourceFile,
					dictionaryID, resourceAccessTool.getToolResource()
							.getResourceStructure());
		} catch (IOException e) {
			logger.error(
					"** PROBLEM ** Cannot create Mgrep file for exporting resource "
							+ resourceAccessTool.getToolResource()
									.getResourceName(), e);
		}
		return mgrepResourceFile;
	}

	/**
	 * For annotations with concepts from ontologies that already exist in the
	 * resource, annotations are reported to the _ET table in the form of
	 * localConceptIDs separated by '> '. This function transfers the reported
	 * annotation into the corresponding _DAT table in order for them to be
	 * available in the same format and to be processed by the rest of the
	 * workflow (semantic expansion). It use the dictionaryID of the given
	 * dictionary. Returns the number of reported annotations added to _DAT.
	 */
	private long reportExistingAnnotations(DictionaryBean dictionary) {
		long nbReported;
		ExecutionTimer timer = new ExecutionTimer();

		logger.info("\t** Processing of existing reported annotations...");
		timer.start();
 
		nbReported = addExistingAnnotations(dictionary.getDictionaryId(),
				resourceAccessTool.getToolResource()
						.getResourceStructure());

		timer.end();
		logger.info("\t## " +nbReported + " reported annotations processed in: "
				+ timer.millisecondsToTimeString(timer.duration()));

		return nbReported;
	}
	
	/**
	 * 
	 * @param dictionaryID
	 * @param structure
	 * @return
	 */
	public long addExistingAnnotations(int dictionaryID, Structure structure){
		long nbAnnotations =0; 
		for(String contextName: structure.getContextNames()){
			// we must exclude contexts NOT_FOR_ANNOTATION and contexts FOR_CONCEPT_RECOGNITION 
			if(!structure.getOntoID(contextName).equals(Structure.FOR_CONCEPT_RECOGNITION) &&
					!structure.getOntoID(contextName).equals(Structure.NOT_FOR_ANNOTATION)){
				boolean isNewVersionOntlogy = ontologyDao.hasNewVersionOfOntology(structure.getOntoID(contextName), structure.getResourceId());
				String localOntologyID = ontologyDao.getLatestLocalOntologyID(structure.getOntoID(contextName));
				nbAnnotations += elementTableDao.addExistingAnnotations(dictionaryID, structure, contextName, localOntologyID, isNewVersionOntlogy, directAnnotationTableDao);				
			}
			
		}
		return nbAnnotations;
	}

	/**
	 * Method removes annotations for given ontology versions.
	 * 
	 * <p>For big resources, it remove local ontology id one by one
	 * <p>For other resources remove all local ontology ids
	 * 
	 * @param {@code List} of localOntologyIDs String containing ontology versions.
	 */
	public void removeAnnotations(List<String> localOntologyIDs) {
		
//		if(resourceAccessTool.getResourceType()!= ResourceType.BIG){
			 directAnnotationTableDao.deleteEntriesFromOntologies(localOntologyIDs);	 
//		 }else{
//			 for (String localOntologyID : localOntologyIDs) {
//				 directAnnotationTableDao.deleteEntriesFromOntology(localOntologyID);	 
//			}
//			 
//		 }
	}

	/**
	 * This method creates temporary element table used for annotation for non annotated
	 * element for given dictionary id.
	 * 
	 * @param dictionaryID 
	 * @return Number of rows containing in temporary table
	 */ 
	public long createTemporaryElementTable(int dictionaryID) {
		 return elementTableDao.createTemporaryTable(dictionaryID, resourceAccessTool.getMaxNumberOfElementsToProcess());		
	}
	
	public void createIndexForAnnotationTable() {	
		if(!directAnnotationTableDao.isIndexExist()){
			 directAnnotationTableDao.createIndex();
			 logger.info("\tIndexes created for table " + directAnnotationTableDao.getTableSQLName());
		} else{
			logger.info("\tIndexes already present in table " + directAnnotationTableDao.getTableSQLName());
		}
	}

	public int getNumberOfElementsForAnnotation(int dictionaryID) {		 
		return elementTableDao.numberOfElementsForMgrepAnnotation(dictionaryID);
	}

	/**
	 * Disable indexes for all annotation tables
	 * 
	 * @return
	 */
	public boolean disableIndexes() {		
		return directAnnotationTableDao.disableIndexes()
				&& isaExpandedAnnotationTableDao.disableIndexes()
				&& mapExpandedAnnotationTableDao.disableIndexes();
	}

	/**
	 *  Enable indexes for all annotation table
	 *   
	 * @return
	 */
	public boolean enableIndexes(boolean bigResource) {	
		return directAnnotationTableDao.enableIndexes(bigResource)
			&& isaExpandedAnnotationTableDao.enableIndexes(bigResource)
			&& mapExpandedAnnotationTableDao.enableIndexes(bigResource);
		 
	}
}

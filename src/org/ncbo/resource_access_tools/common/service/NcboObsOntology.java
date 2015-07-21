package org.ncbo.resource_access_tools.common.service;

import java.io.StringWriter;
import java.util.List;

import org.apache.log4j.Logger;
import org.ncbo.resource_access_tools.common.beans.ObrResultBean;

import com.hp.hpl.jena.ontology.OntModel;

import edu.stanford.smi.protegex.owl.ProtegeOWL;
import edu.stanford.smi.protegex.owl.jena.JenaOWLModel;
import edu.stanford.smi.protegex.owl.model.OWLIndividual;
import edu.stanford.smi.protegex.owl.model.RDFSLiteral;
import edu.stanford.smi.protegex.owl.model.RDFUntypedResource;

/**
* This class is a an object representation of the NCBO OBS OWL ontology.
*  
* @author Clement Jonquet
* @created July-2009
*/
public class NcboObsOntology {

	private static final String FILE_NAME = "NCBO_OBS_ontology.owl"; 
	private static final String URI = "http://obs.bioontology.org/ontologies/"+FILE_NAME ;
	
	public static final String NAME_SPACE = "URI" + "#";
	public JenaOWLModel owlModel;
	public RDFUntypedResource namespace;
	
	// OWL values for class name
	public static final String OWL_OBR_RESULT = "ResourceIndexResult";
	public static final String OWL_OBA_RESULT = "AnnotatorResult";
	public static final String OWL_STATISTICS = "Statistics";
	public static final String OWL_DICTIONARY = "Dictionary";
	public static final String OWL_PARAMETERS = "Parameters";
	public static final String OWL_ANNOTATION = "Annotation";
	public static final String OWL_OBR_ANNOTATION = "ResourceIndexAnnotation";
	public static final String OWL_ONTOLOGY_USED = "OntologyUsed";
	public static final String OWL_CONCEPT = "Concept";
	public static final String OWL_REPORTED_CONTEXT = "ReportedContext";
	public static final String OWL_MGREP_CONTEXT = "MgrepContext";
	public static final String OWL_ISA_CONTEXT = "IsaContext";
	public static final String OWL_MAPPING_CONTEXT = "MappingContext";
	public static final String OWL_DISTANCE_CONTEXT = "DistanceContext";
	
	// OWL values for object property name
	public static final String OWL_HAS_RESULT = "has_result";
	public static final String OWL_HAS_STATISTICS = "has_statistics";
	public static final String OWL_HAS_DICTIONARY = "has_dictionary";
	public static final String OWL_HAS_PARAMETERS = "has_parameters";
	public static final String OWL_HAS_ANNOTATION = "has_annotation";
	public static final String OWL_HAS_ONTOLOGY_USED = "has_ontologyUsed";
	public static final String OWL_HAS_CONCEPT = "has_concept";
	public static final String OWL_HAS_CONTEXT = "has_context";
	
	// OWL values for datatype property name
	public static final String OWL_CONTEXT_NAME = "contextName";
	public static final String OWL_NB_ANNOTATION = "annotationCount";
	public static final String OWL_LONGEST_ONLY = "longestOnly";
	public static final String OWL_WHOLEWORD_ONLY = "wholeWordOnly";
	public static final String OWL_STOP_WORDS = "stopWord";
	public static final String OWL_MIN_TERM_SIZE = "minTermSize";
	public static final String OWL_SCORED = "scored";
	public static final String OWL_WITH_SYN = "withSynonyms";
	public static final String OWL_LOCAL_ONTOID = "localOntologyId";
	public static final String OWL_ONTO_TO_KEEP_IN_RESULT = "ontologyToKeepInResult";
	public static final String OWL_LOCAL_STT = "localSemanticTypeId";
 	public static final String OWL_LEVEL_MAX = "levelMax";
	public static final String OWL_MAPPING_TYPE = "mappingType";
	public static final String OWL_MORE_STOP_WORD = "additionalStopWord";
	public static final String OWL_CASE_SENSITIVE_STOP_WORD = "isStopWordsCaseSenstive";	
	public static final String OWL_SCORE = "score";
	public static final String OWL_LOCAL_ONTOLOGYID = "localOntologyId";
	public static final String OWL_ONTOLOGY_NAME = "ontologyName";
	public static final String OWL_ONTOLOGY_VERSION = "ontologyVersion";
	public static final String OWL_VIRTUAL_ONTOLOGYID = "virtualOntologyId";
	public static final String OWL_LOCAL_ELEMENTID = "localElementId";
	public static final String OWL_LOCAL_CONCEPTID = "localConceptId";
	public static final String OWL_ELEMENTID = "elementid";
	public static final String OWL_CONCEPTID = "conceptid";
	public static final String OWL_RESOURCEID = "resourceid";
	public static final String OWL_MODE = "mode";
	public static final String OWL_WITH_CONTEXT = "withContext";
	public static final String OWL_ELEMENT_DETAILS = "elementDetails";
	public static final String OWL_COUNTS = "counts";
	public static final String OWL_OFFSET_START = "offsetStart";
	public static final String OWL_OFFSET_MAX = "offsetMax";
	public static final String OWL_TEXT_TO_ANNOTATE = "textToAnnotate";
	public static final String OWL_DICTIONARY_ID = "dictionaryId";
	public static final String OWL_DICTIONARY_NAME = "dictionaryName";
	public static final String OWL_DICTIONARY_DATE = "dictionaryDate";
	public static final String OWL_PREFERRED_NAME = "preferredName";
	public static final String OWL_SYNONYM = "synonym";
	public static final String OWL_IS_ROOT = "isRoot";
	public static final String OWL_IS_DIRECT = "isDirect";
	public static final String OWL_TERMID = "termid";
	public static final String OWL_TERM_NAME = "termName";
	public static final String OWL_FROM = "from";
	public static final String OWL_TO = "to";
	public static final String OWL_LEVEL = "level";
	public static final String OWL_DISTANCE = "distance";
	public static final String OWL_EMAIL = "email"; 
	public static final String OWL_APPLICATIONID="applicationid";
	public static final String OWL_FORMAT="format";
	public static final String OWL_FILTERNUMBER="filterNumber";
	
	public static final String OWL_WITH_DEFAULT_STOP_WORD="withDefaultStopWords";
	public static final String OWL_IS_VIRTUAL_ONTOLOGY_ID="isVirtualOntologyId";	
	
	// Logger for this class
	private static Logger logger = Logger.getLogger(NcboObsOntology.class);
	
	private NcboObsOntology(){
		try {
			this.owlModel = ProtegeOWL.createJenaOWLModelFromURI(URI);
			this.namespace = this.owlModel.createRDFUntypedResource(NAME_SPACE);
			logger.info("Loading of the ontology complete.");
		} catch (Exception e) {
			e.printStackTrace();
			logger.error("** PROBLEM ** Cannot create the OWL model for NCBOObsOntology.");
		}
	}

	private static class NcboObsOntologyMainHolder {
		private final static NcboObsOntology INSTANCE = new NcboObsOntology();
	}
	
	public static NcboObsOntology getInstance(){
		return NcboObsOntologyMainHolder.INSTANCE;
	}
	
	public static String getFILE_NAME() {
		return FILE_NAME;
	}

	public static String getURI() {
		return URI;
	}

	public static String getNAME_SPACE() {
		return NAME_SPACE;
	}
		
	public void removeInstances(){
		/*
		Collection<OWLIndividual> allInd = this.owlModel.getOWLIndividuals();
		for(Iterator<OWLIndividual> it = allInd.iterator(); it.hasNext();){
			it.next().delete();
		}
		*/
		for(Object o: this.owlModel.getOWLIndividuals()){
			OWLIndividual individual = (OWLIndividual)o; 
			individual.delete();
		}
	} 
	
	/**
	 * Converts the given int to a RDFS literal poistiveInteger. 
	 */
	public RDFSLiteral convertToPositiveInteger(int integer){
		// creates an RDFDatatype and a RDFLiteral for the positiveInteger
		return this.owlModel.createRDFSLiteral(Integer.toString(integer), this.owlModel.getRDFSDatatypeByName("xsd:positiveInteger"));
	}
	
	/**
	 *	Returns a string that contains all the result content in an OWL ontology populated with instances.
	 *	http://obs.bioontology.org/ontologies/NCBO_OBS_ontology.owl
	 */
	public String toOWL(List<ObrResultBean> resultBeans){
		String owlContent = "";
		try{
			
			for (ObrResultBean resultBean : resultBeans) {
				// creates a new Result individual
				resultBean.getOWLIndividual(this);
			} 
			// writes the corresponding populated ontology to a stream
			OntModel ontModel = owlModel.getOntModel();
			StringWriter fstream = new StringWriter();
			fstream.write("<?xml version=\"1.0\"?>\n");
			ontModel.write(fstream, "RDF/XML-ABBREV");
			fstream.close();
			owlContent = fstream.toString();			
			// removes instances from the static ontology (for next use)
			this.removeInstances(); 
		}
		catch (Exception e) {
			e.printStackTrace();
			System.out.println("** PROBLEM ** Cannot export ResultBean as OWL file. Empty file returned.");
		}
		return owlContent;
	}
}

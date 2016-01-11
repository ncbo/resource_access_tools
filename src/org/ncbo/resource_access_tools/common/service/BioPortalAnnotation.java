package org.ncbo.resource_access_tools.common.service;

import java.io.ByteArrayOutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.List;

import org.apache.log4j.Logger;
import org.ncbo.resource_access_tools.common.utils.Utilities;

import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.util.FileUtils;

import edu.stanford.smi.protegex.owl.ProtegeOWL;
import edu.stanford.smi.protegex.owl.jena.JenaOWLModel;
import edu.stanford.smi.protegex.owl.model.RDFIndividual;
import edu.stanford.smi.protegex.owl.model.RDFSLiteral;
import edu.stanford.smi.protegex.owl.model.RDFUntypedResource;

/**
* This class is a an object representation of the Bio Portal Annotation.
* Using the RDFS ontology at http://obs.bioontology.org/ontologies/BioPortalAnnotation.rdfs
*
* @author Kuladip Yadav
*/
public class BioPortalAnnotation {

	private static final String FILE_NAME = "BioPortalAnnotation.rdfs";
	private static final String URI = "http://obs.bioontology.org/ontologies/"+FILE_NAME ;

	private static final String RESOURCE_INDEX_BASE_URL ="http://rest.bioontology.org/resource_index";
	private static final String BIOPORTAL_BASE_URL = "http://rest.bioontology.org/bioportal";

	public static final String NAME_SPACE = URI + "#";
	public static final String RDF_RESOURCE_INDEX_ANNOTATION = "ResourceIndexAnnotation";

	// RDF values for object property name
	public static final String RDF_ANNOTATED_ELEMENT= "annotatedElement";
	public static final String RDF_ANNOTATION_CONTEXT= "annotationContext";

	public static final String RDF_REPORTED_CONTEXT = "ReportedContext";
	public static final String RDF_MGREP_CONTEXT = "MgrepContext";
	public static final String RDF_ISA_CONTEXT = "IsaContext";
	public static final String RDF_MAPPING_CONTEXT = "MappingContext";

	public static final String RDF_ANNOTATION_CONCEPT= "annotatingConcept";
	public static final String RDF_CONTEXT_NAME = "contextName";
	public static final String RDF_IS_DIRECT = "isDirect";
	public static final String RDF_TERMID = "termid";
	public static final String RDF_TERM_NAME = "termName";
	public static final String RDF_FROM = "from";
	public static final String RDF_TO = "to";
	public static final String RDF_CHILD_CONCEPT_ID = "childConceptId";
	public static final String RDF_MAPPED_CONCEPT_ID = "mappedConceptId";
	public static final String RDF_MAPPING_TYPE = "mappingType";
	public static final String RDF_LEVEL = "level";
	public static final String RDF_DISTANCE = "distance";

	public JenaOWLModel owlModel;
	public RDFUntypedResource namespace;

	// Logger for this class
	private static Logger logger = Logger.getLogger(BioPortalAnnotation.class);

	private BioPortalAnnotation(){
		try {
			this.owlModel = ProtegeOWL.createJenaOWLModelFromURI(URI);
			this.namespace = this.owlModel.createRDFUntypedResource(NAME_SPACE);
			logger.info("Loading of the Bio Portal Annotation complete.");
		} catch (Exception e) {
			e.printStackTrace();
			logger.error("** PROBLEM ** Cannot create the OWL model for BioPortalAnnotation.");
		}
	}

	private static class BioPortalAnnotationHolder {
		private final static BioPortalAnnotation INSTANCE = new BioPortalAnnotation();
	}

	public static BioPortalAnnotation getInstance(){
		return BioPortalAnnotationHolder.INSTANCE;
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
		for(Object o: this.owlModel.getRDFIndividuals()){
			RDFIndividual individual = (RDFIndividual)o;
			individual.delete();
		}
	}

	/**
	 *	Returns a string that contains all the result content in RDF bioportal Annotations populated with instances.
	 *	http://obs.bioontology.org/ontologies/BioPortalAnnotation.rdfs
	 */
	public String toRDF(List<ObrResultBean> resultBeans){
		String owlContent = "";
		try{
			for (ObrResultBean resultBean : resultBeans) {
				// creates a new Result individual
				resultBean.getRDFIndividual(this);
			}
			// writes the corresponding populated ontology to a stream
			OntModel ontModel = owlModel.getOntModel();
		    ByteArrayOutputStream bos= new ByteArrayOutputStream();
            JenaOWLModel.saveModel(bos, ontModel, FileUtils.langXMLAbbrev, NAME_SPACE, URI);
			owlContent = bos.toString();
			bos.close();
			// removes instances from the static ontology (for next use)
			this.removeInstances();
		}
		catch (Exception e) {
			e.printStackTrace();
			System.out.println("** PROBLEM ** Cannot export list of ResultBean as RDF file. Empty file returned.");
		}
		return owlContent;
	}

	/**
	 * Converts the given int to a RDFS literal poistiveInteger.
	 */
	public RDFSLiteral convertToPositiveInteger(int integer){
		// creates an RDFDatatype and a RDFLiteral for the positiveInteger
		return this.owlModel.createRDFSLiteral(Integer.toString(integer), this.owlModel.getRDFSDatatypeByName("xsd:positiveInteger"));
	}

	/**
	 * Converts the given String to a RDFS literal anyURI.
	 */
	public RDFSLiteral convertToURI(String uri){
		// creates an RDFDatatype and a RDFLiteral for the positiveInteger
		return this.owlModel.createRDFSLiteral(uri, this.owlModel.getRDFSDatatypeByName("xsd:anyURI"));
	}

	/**
	 * Create element url for given element id for given resource and Converts the given String to a RDFS literal anyURI.
	 *
	 * @param resourceId
	 * @param elementId
	 * @return
	 */
	public RDFSLiteral createAnnotatedElementURL(String resourceId, String elementId){
		String encodedElementId;
		 try {
			 encodedElementId= URLEncoder.encode(elementId, Utilities.UTF8_STRING);
		} catch (UnsupportedEncodingException e) {
			encodedElementId= elementId;
		}
		return convertToURI(RESOURCE_INDEX_BASE_URL + "/element/" + resourceId + "?elementid=" + encodedElementId);
	}

	/**
	 * Create concept URL for given localConceptId and converts the given String to a RDFS literal anyURI.
	 *
	 * @param localConceptId
	 * @return
	 */
	public RDFSLiteral createAnnotatedConceptURL(String localConceptId){
		String[] splittedConcept= Utilities.splitSecure(localConceptId, Utilities.SLASH_STRING, 2);
		if(splittedConcept != null && splittedConcept.length==2){
			try {
				return convertToURI(BIOPORTAL_BASE_URL + "/rdf/" + splittedConcept[0] + "/?conceptid=" + URLEncoder.encode(splittedConcept[1], Utilities.UTF8_STRING));
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
			}
		}
		return null;
	}

}

package org.ncbo.resource_access_tools.common.beans;

import java.util.ArrayList;
import java.util.HashSet;

import org.ncbo.resource_access_tools.common.service.BioPortalAnnotation;
import org.ncbo.resource_access_tools.common.service.NcboObsOntology;

import edu.stanford.smi.protegex.owl.model.OWLIndividual;
import edu.stanford.smi.protegex.owl.model.RDFIndividual;

/**
 * @author Clement Jonquet
 * @version 1.0
 * @created 22-Feb-2009 2:42:12 PM
 */
public class ObrResultBeanDetailled extends ObrResultBean {

	private ArrayList<ObrAnnotationBean> mgrepAnnotations;
	private ArrayList<ObrAnnotationBean> reportedAnnotations;
	private ArrayList<ObrAnnotationBean> isaAnnotations;
	private ArrayList<ObrAnnotationBean> mappingAnnotations;
	
	public ObrResultBeanDetailled(String resultId, DictionaryBean dictionary,
			HashSet<StatisticsBean> statistics, ParametersBean parameters,
			ArrayList<ObrAnnotationBean> mgrepAnnotations,
			ArrayList<ObrAnnotationBean> reportedAnnotations,
			ArrayList<ObrAnnotationBean> isaAnnotations,
			ArrayList<ObrAnnotationBean> mappingAnnotations,
			HashSet<String> localConceptIds, String localElementId,
			String resourceId, String mode,
			boolean withContext, boolean elementDetails, boolean counts, 
			int offsetStart, int offsetMax) {
		super(resultId, dictionary, statistics, parameters, new ArrayList<ObrAnnotationBean>(), localElementId, localConceptIds, resourceId, mode, withContext, elementDetails, counts, offsetStart, offsetMax);
		this.mgrepAnnotations = mgrepAnnotations;
		this.reportedAnnotations = reportedAnnotations;
		this.isaAnnotations = isaAnnotations;
		this.mappingAnnotations = mappingAnnotations;
	} 

	/**
	 * @return the mgrepAnnotations
	 */
	public ArrayList<ObrAnnotationBean> getMgrepAnnotations() {
		return mgrepAnnotations;
	}
	
	/**
	 * @return the reportedAnnotations
	 */
	public ArrayList<ObrAnnotationBean> getReportedAnnotations() {
		return reportedAnnotations;
	}



	public ArrayList<ObrAnnotationBean> getIsaAnnotations() {
		return isaAnnotations;
	}

	public ArrayList<ObrAnnotationBean> getMappingAnnotations() {
		return mappingAnnotations;
	}
	
	/** The corresponding format is:
	 * localElementId \t score \t localConceptId \t preferredName \t synonyms (separated by ' /// ') \t localSemanticTypeId (separated by ' /// ')
	 * 	\t contextName \t isDirect \t other context information (e.g., childConceptId, mappedConceptId, level, mappingType) (separated by ' /// ')
	 * (note end of line is done with \r\n characters).
	 */
	public String toTabDelimited(){
		StringBuffer lineb = new StringBuffer();
		
		// Adding mgrep annotations
		for(ObrAnnotationBean annotation: this.mgrepAnnotations){	
			lineb.append(getResourceId());
			lineb.append("\t");
			lineb.append(annotation.toTabDelimited());
			lineb.append("\r\n");
		}
		
		// Adding resported annotations
		for(ObrAnnotationBean annotation: this.reportedAnnotations){	
			lineb.append(getResourceId());
			lineb.append("\t");
			lineb.append(annotation.toTabDelimited());
			lineb.append("\r\n");
		}
		
		// Adding is a annotations
		for(ObrAnnotationBean annotation: this.isaAnnotations){	
			lineb.append(getResourceId());
			lineb.append("\t");
			lineb.append(annotation.toTabDelimited());
			lineb.append("\r\n");
		}
		
		// Adding Mapping annotation
		for(ObrAnnotationBean annotation: this.mappingAnnotations){	
			lineb.append(getResourceId());
			lineb.append("\t");
			lineb.append(annotation.toTabDelimited());
			lineb.append("\r\n");
		} 
		
		return lineb.toString();
	}

	/**
	 * Returns a OWL individual for that object and creates the necessary other individuals, in the given ontology. 
	 */
	@Override
	public OWLIndividual getOWLIndividual(NcboObsOntology obsOntology){

		// creates the class individual
		OWLIndividual individual = super.getOWLIndividual(obsOntology);
		
		// assigns the individual properties
		for (ObrAnnotationBean annotation: this.mgrepAnnotations){
			individual.addPropertyValue(obsOntology.owlModel.getOWLObjectProperty(NcboObsOntology.OWL_HAS_ANNOTATION), annotation.getOWLIndividual(obsOntology, individual));
		}
		for (ObrAnnotationBean annotation: this.reportedAnnotations){
			individual.addPropertyValue(obsOntology.owlModel.getOWLObjectProperty(NcboObsOntology.OWL_HAS_ANNOTATION), annotation.getOWLIndividual(obsOntology, individual));
		}
		for (ObrAnnotationBean annotation: this.isaAnnotations){
			individual.addPropertyValue(obsOntology.owlModel.getOWLObjectProperty(NcboObsOntology.OWL_HAS_ANNOTATION), annotation.getOWLIndividual(obsOntology, individual));
		}
		for (ObrAnnotationBean annotation: this.mappingAnnotations){
			individual.addPropertyValue(obsOntology.owlModel.getOWLObjectProperty(NcboObsOntology.OWL_HAS_ANNOTATION), annotation.getOWLIndividual(obsOntology, individual));
		}
		
		return individual;
	}
	
	/**
	 * Returns a RDF individual for that object and creates the necessary other individuals, in the given ontology. 
	 */
	public RDFIndividual getRDFIndividual(
			BioPortalAnnotation bioPortalAnnotation) {		 
		 
		// assigns the individual properties
		for (ObrAnnotationBean annotation: this.mgrepAnnotations){
			annotation.getRDFIndividual(bioPortalAnnotation, this.getResourceId()); 
		}
		for (ObrAnnotationBean annotation: this.reportedAnnotations){
			annotation.getRDFIndividual(bioPortalAnnotation, this.getResourceId()); 
		}
		for (ObrAnnotationBean annotation: this.isaAnnotations){
			annotation.getRDFIndividual(bioPortalAnnotation, this.getResourceId() ); 
		}
		for (ObrAnnotationBean annotation: this.mappingAnnotations){
			annotation.getRDFIndividual(bioPortalAnnotation, this.getResourceId() ); 
		}
		
		return null;
	}
	
	/**
	 * Constructs a <code>String</code> with all attributes in name = value format.
	 * @return a <code>String</code> representation of this object.
	 */
	public String toString(){
	    final String TAB = "\r\n";
	    StringBuffer retValue = new StringBuffer();
	    retValue.append("resultDetailed [ ").append(TAB)
	        .append(super.toString()).append(TAB)
	        .append("mgrepAnnotations = ").append(this.mgrepAnnotations).append(TAB)
	         .append("reportedAnnotations = ").append(this.reportedAnnotations).append(TAB)
	        .append("isaAnnotations = ").append(this.isaAnnotations).append(TAB)
	        .append("mappingAnnotations = ").append(this.mappingAnnotations).append(TAB)
	        .append("]");
	    return retValue.toString();
	}
	
	
	
}

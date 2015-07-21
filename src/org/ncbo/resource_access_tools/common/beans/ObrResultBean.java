package org.ncbo.resource_access_tools.common.beans;

import java.util.ArrayList;
import java.util.HashSet;

import org.ncbo.resource_access_tools.common.service.BioPortalAnnotation;
import org.ncbo.resource_access_tools.common.service.NcboObsOntology;

import edu.stanford.smi.protegex.owl.model.OWLIndividual;
import edu.stanford.smi.protegex.owl.model.RDFIndividual;

/**
 * @author jonquet
 * @version 1.0
 * @created 24-Sep-2008 2:42:12 PM
 */
public class ObrResultBean extends ResultBean {
	
	private ArrayList<ObrAnnotationBean> annotations;
	private String localElementId;
	private HashSet<String> localConceptIds;
	private String resourceId;
	private String mode;
	private boolean withContext;
	private boolean elementDetails;
	private boolean counts;
	private int offsetStart;
	private int offsetMax;
		
	public ObrResultBean(String resultId, DictionaryBean dictionary,
			HashSet<StatisticsBean> statistics, ParametersBean parameters,
			ArrayList<ObrAnnotationBean> annotations,
			String localElementId, HashSet<String> localConceptIds,
			String resourceId, String mode, boolean withContext,
			boolean elementDetails, boolean counts, int offsetStart,
			int offsetMax) {
		super(resultId, dictionary, statistics, parameters);
		this.annotations = annotations;
		this.localElementId = localElementId;
		this.localConceptIds = localConceptIds;
		this.resourceId = resourceId;
		this.mode = mode;
		this.withContext = withContext;
		this.elementDetails = elementDetails;
		this.counts = counts;
		this.offsetStart = offsetStart;
		this.offsetMax = offsetMax;
	}

	public ArrayList<ObrAnnotationBean> getAnnotations() {
		return annotations;
	}

	public String getLocalElementId() {
		return localElementId;
	}

	public HashSet<String> getLocalConceptIds() {
		return localConceptIds;
	}

	public String getResourceId() {
		return resourceId;
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

	public int getOffsetStart() {
		return offsetStart;
	}

	public int getOffsetMax() {
		return offsetMax;
	}
	
	/** The corresponding format is:
	 * localElementId \t score \t localConceptId \t preferredName \t synonyms (separated by ' /// ') \t localSemanticTypeId (separated by ' /// ')
	 * 	\t contextName \t isDirect \t other context information (e.g., childConceptId, mappedConceptId, level, mappingType) (separated by ' /// ')
	 * (note end of line is done with \r\n characters).
	 */
	public String toTabDelimited(){
		StringBuffer lineb = new StringBuffer();
		// for each annotations
		for(ObrAnnotationBean annotation: this.annotations){
			lineb.append(resourceId);
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
		OWLIndividual individual = obsOntology.owlModel.getOWLNamedClass(NcboObsOntology.OWL_OBR_RESULT).createOWLIndividual(this.getResultId());

		// assigns the individual properties (super class)
		this.assignPropertyValue(individual, obsOntology);
		
		// assigns the individual properties
		for (ObrAnnotationBean annotation: this.annotations){
			individual.addPropertyValue(obsOntology.owlModel.getOWLObjectProperty(NcboObsOntology.OWL_HAS_ANNOTATION), annotation.getOWLIndividual(obsOntology, individual));
		}
		if(this.localElementId != null){
			individual.addPropertyValue(obsOntology.owlModel.getOWLDatatypeProperty(NcboObsOntology.OWL_LOCAL_ELEMENTID), this.localElementId);
		}
		for (String localConceptId: this.localConceptIds){
			individual.addPropertyValue(obsOntology.owlModel.getOWLDatatypeProperty(NcboObsOntology.OWL_LOCAL_CONCEPTID), localConceptId);
		}
		individual.addPropertyValue(obsOntology.owlModel.getOWLDatatypeProperty(NcboObsOntology.OWL_RESOURCEID), this.resourceId);
		
		if(this.mode != null){
			individual.addPropertyValue(obsOntology.owlModel.getOWLDatatypeProperty(NcboObsOntology.OWL_MODE), String.valueOf(this.mode));
		}
		individual.addPropertyValue(obsOntology.owlModel.getOWLDatatypeProperty(NcboObsOntology.OWL_WITH_CONTEXT), Boolean.valueOf(this.withContext));
		individual.addPropertyValue(obsOntology.owlModel.getOWLDatatypeProperty(NcboObsOntology.OWL_ELEMENT_DETAILS), Boolean.valueOf(this.elementDetails));
		individual.addPropertyValue(obsOntology.owlModel.getOWLDatatypeProperty(NcboObsOntology.OWL_COUNTS), Boolean.valueOf(this.counts));
		individual.addPropertyValue(obsOntology.owlModel.getOWLDatatypeProperty(NcboObsOntology.OWL_OFFSET_START), Integer.valueOf(this.offsetStart));
		individual.addPropertyValue(obsOntology.owlModel.getOWLDatatypeProperty(NcboObsOntology.OWL_OFFSET_MAX), Integer.valueOf(this.offsetMax));
		
		return individual;
	}
	
	/**
	 * Returns a RDF individual for that object and creates the necessary other individuals, in the given ontology. 
	 */
	public RDFIndividual getRDFIndividual(
			BioPortalAnnotation bioPortalAnnotation) { 
		 
		// assigns the individual properties
		for (ObrAnnotationBean annotation: this.annotations){
			annotation.getRDFIndividual(bioPortalAnnotation, this.resourceId); 
		}		 		
		return null;
	}

	/**
	 * Constructs a <code>String</code> with all attributes
	 * in name = value format.
	 *
	 * @return a <code>String</code> representation 
	 * of this object.
	 */
	public String toString() {
		final String TAB = "\r\n";
	    StringBuffer retValue = new StringBuffer();
	    retValue.append("result [ ").append(TAB)
	        .append("\tlocalElementId = ").append(this.localElementId).append(", ")
	        .append("\tlocalConceptIds = ").append(this.localConceptIds).append(TAB)
	        .append("resourceId = ").append(this.resourceId).append(", ")
	        .append("mode = ").append(this.mode).append(", ")
	        .append("withContext = ").append(this.withContext).append(", ")
	        .append("elementDetails = ").append(this.elementDetails).append(", ")
	        .append("counts = ").append(this.counts).append(", ")
	        .append("offsetStart = ").append(this.offsetStart).append(", ")
	        .append("offsetMax = ").append(this.offsetMax).append(TAB)
	    	.append(super.toString()).append(TAB)
	        .append("annotations = ").append(this.annotations).append(TAB)
	        .append("]");
	    return retValue.toString();
	} 
}
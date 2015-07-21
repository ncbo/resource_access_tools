package org.ncbo.resource_access_tools.common.beans;

import org.ncbo.resource_access_tools.common.service.BioPortalAnnotation;
import org.ncbo.resource_access_tools.common.service.NcboObsOntology;

import edu.stanford.smi.protegex.owl.model.OWLIndividual;
import edu.stanford.smi.protegex.owl.model.RDFIndividual;


/**
 * @author jonquet
 * @version 1.0
 * @created 24-Sep-2008 2:42:12 PM
 */
public class MappingContextBean extends ContextBean {

	public static final String MAPPING_CTX = "MAPPING";
	public static final int MEA_WEIGHT = 7; 
	
	private String mappedConceptId;
	private String mappingType;
	private int from;
	private int to;
	
	public MappingContextBean(String contextName, String mappedConceptId, String mappingType, int from, int to) {
		super(contextName, false);
		this.mappedConceptId = mappedConceptId;
		this.mappingType = mappingType;
		this.from = from;
		this.to = to;
	}

	public String getMappedConceptId() {
		return mappedConceptId;
	}

	public String getMappingType() {
		return mappingType;
	}

	public String getOtherContextInformation(){
		return this.mappedConceptId + "///" + this.mappingType;
	}


	public int getFrom() {
		return from;
	}

	public int getTo() {
		return to;
	}

	/**
	 * Returns a OWL individual for that object and creates the necessary other individuals, in the given ontology. 
	 */
	@Override
	public OWLIndividual getOWLIndividual(NcboObsOntology obsOntology, OWLIndividual annotationInd, OWLIndividual resultInd){

		// creates the class individual
		OWLIndividual individual = obsOntology.owlModel.getOWLNamedClass(NcboObsOntology.OWL_MAPPING_CONTEXT).createOWLIndividual(this.generateOWLName(annotationInd, resultInd));

		// assigns the individual properties
		individual.addPropertyValue(obsOntology.owlModel.getOWLObjectProperty(NcboObsOntology.OWL_HAS_ANNOTATION), annotationInd);
		individual.addPropertyValue(obsOntology.owlModel.getOWLDatatypeProperty(NcboObsOntology.OWL_LOCAL_CONCEPTID), this.mappedConceptId);
		individual.addPropertyValue(obsOntology.owlModel.getOWLDatatypeProperty(NcboObsOntology.OWL_MAPPING_TYPE), this.mappingType);
		individual.addPropertyValue(obsOntology.owlModel.getOWLDatatypeProperty(NcboObsOntology.OWL_FROM), Integer.valueOf(this.from));
		individual.addPropertyValue(obsOntology.owlModel.getOWLDatatypeProperty(NcboObsOntology.OWL_TO), Integer.valueOf(this.to));

		this.assignPropertyValue(individual, obsOntology);
		
		return individual;
	}
	
	/**
	 * Returns a RDF individual for that object and creates the necessary other individuals, in the given ontology. 
	 */
	@Override
	public RDFIndividual getRDFIndividual(BioPortalAnnotation bioPortalAnnotation, RDFIndividual annotationIndividual) {
		// creates the class individual
		RDFIndividual individual = bioPortalAnnotation.owlModel.getRDFSNamedClass(BioPortalAnnotation.RDF_MAPPING_CONTEXT).createRDFIndividual(this.generateRDFName(annotationIndividual));

		// assigns the individual properties
		if(this.mappedConceptId != 	null){
			individual.addPropertyValue(bioPortalAnnotation.owlModel.getRDFProperty(BioPortalAnnotation.RDF_MAPPED_CONCEPT_ID), bioPortalAnnotation.createAnnotatedConceptURL(this.mappedConceptId));
		}		
		individual.addPropertyValue(bioPortalAnnotation.owlModel.getRDFProperty(BioPortalAnnotation.RDF_MAPPING_TYPE), this.mappingType);
		individual.addPropertyValue(bioPortalAnnotation.owlModel.getRDFProperty(BioPortalAnnotation.RDF_FROM), Integer.valueOf(this.from));
		individual.addPropertyValue(bioPortalAnnotation.owlModel.getRDFProperty(BioPortalAnnotation.RDF_TO), Integer.valueOf(this.to));
		this.assignPropertyValue(individual, bioPortalAnnotation);
		
		return individual;
	}

	/**
	 * Constructs a <code>String</code> with all attributes in name = value format.
	 * @return a <code>String</code> representation of this object.
	 */
	public String toString(){
	    final String TAB = ", ";
	    StringBuffer retValue = new StringBuffer();
	    retValue.append("[")
	        .append(super.toString()).append(TAB)
	        .append("mappedConceptId = ").append(this.mappedConceptId).append(TAB)
	        .append("mappingType = ").append(this.mappingType).append(TAB)
	        .append("from = ").append(this.from).append(TAB)
	        .append("to = ").append(this.to)
	        .append("]");
	    return retValue.toString();
	} 
}
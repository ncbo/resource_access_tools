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
public class IsaContextBean extends ContextBean {

	public static final String ISA_CTX = "ISA_CLOSURE";
	public static final double IEA_FACTOR = 0.2;
	
	private String childConceptId;
	private int level;
	private int from;
	private int to;
	
	public IsaContextBean(String contextName, String childConceptId, int level, int from, int to) {
		super(contextName, false);
		this.childConceptId = childConceptId;
		this.level = level;
		this.from = from;
		this.to = to;
	}
	
	public String getChildConceptId() {
		return childConceptId;
	}
	
	public int getLevel() {
		return level;
	}

	public String getOtherContextInformation(){
		return this.childConceptId + "///" + this.level;
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
		OWLIndividual individual = obsOntology.owlModel.getOWLNamedClass(NcboObsOntology.OWL_ISA_CONTEXT).createOWLIndividual(this.generateOWLName(annotationInd, resultInd));

		// assigns the individual properties
		individual.addPropertyValue(obsOntology.owlModel.getOWLObjectProperty(NcboObsOntology.OWL_HAS_ANNOTATION), annotationInd);
		individual.addPropertyValue(obsOntology.owlModel.getOWLDatatypeProperty(NcboObsOntology.OWL_LOCAL_CONCEPTID), this.childConceptId);
		individual.addPropertyValue(obsOntology.owlModel.getOWLDatatypeProperty(NcboObsOntology.OWL_LEVEL), obsOntology.convertToPositiveInteger(this.level));
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
		RDFIndividual individual = bioPortalAnnotation.owlModel.getRDFSNamedClass(BioPortalAnnotation.RDF_ISA_CONTEXT).createRDFIndividual(this.generateRDFName(annotationIndividual));

		// assigns the individual properties
		if(childConceptId != null){
			individual.addPropertyValue(bioPortalAnnotation.owlModel.getRDFProperty(BioPortalAnnotation.RDF_CHILD_CONCEPT_ID), bioPortalAnnotation.createAnnotatedConceptURL(this.childConceptId));
		} 
		individual.addPropertyValue(bioPortalAnnotation.owlModel.getRDFProperty(BioPortalAnnotation.RDF_LEVEL), Integer.valueOf(this.level));
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
	        .append("childConceptId = ").append(this.childConceptId).append(TAB)
	        .append("level = ").append(this.level).append(TAB)
	        .append("from = ").append(this.from).append(TAB)
	        .append("to = ").append(this.to)
	        .append("]");
	    return retValue.toString();
	} 
}
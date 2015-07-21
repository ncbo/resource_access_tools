package org.ncbo.resource_access_tools.common.beans;

import org.ncbo.resource_access_tools.common.service.BioPortalAnnotation;
import org.ncbo.resource_access_tools.common.service.NcboObsOntology;

import edu.stanford.smi.protegex.owl.model.OWLIndividual;
import edu.stanford.smi.protegex.owl.model.RDFIndividual;


/**
 * @author jonquet
 * @version 1.0
 * @created 24-Sep-2008 2:42:11 PM
 */
public abstract class ContextBean {

	private String contextName;
	private boolean isDirect;
	
	public ContextBean(String contextName, boolean isDirect) {
		super();
		this.contextName = contextName;
		this.isDirect = isDirect;
	}

	public String getContextName() {
		return contextName;
	}
	
	public boolean isDirect() {
		return isDirect;
	}

	public abstract String getOtherContextInformation();

	protected void assignPropertyValue(OWLIndividual individual, NcboObsOntology obsOntology){
		individual.addPropertyValue(obsOntology.owlModel.getOWLDatatypeProperty(NcboObsOntology.OWL_CONTEXT_NAME), this.contextName);
		individual.addPropertyValue(obsOntology.owlModel.getOWLDatatypeProperty(NcboObsOntology.OWL_IS_DIRECT), this.isDirect);
	} 
	
	/**
	 * Returns a OWL individual for that object and creates the necessary other individuals, in the given ontology. 
	 */
	public abstract OWLIndividual getOWLIndividual(NcboObsOntology obsOntology, OWLIndividual annotationInd, OWLIndividual resultInd);
	
	/**
	 * Adding bean properties to RDF individuals
	 * 
	 * @param individual
	 * @param bioPortalAnnotation
	 */
	protected void assignPropertyValue(RDFIndividual individual, BioPortalAnnotation bioPortalAnnotation){
		individual.addPropertyValue(bioPortalAnnotation.owlModel.getRDFProperty(NcboObsOntology.OWL_CONTEXT_NAME), this.contextName);
		individual.addPropertyValue(bioPortalAnnotation.owlModel.getRDFProperty(NcboObsOntology.OWL_IS_DIRECT), this.isDirect);
	} 
	
	/**
	 * Returns a RDF individual for that object and creates the necessary other individuals, in the given ontology. 
	 */
	public abstract RDFIndividual getRDFIndividual(BioPortalAnnotation bioPortalAnnotation, RDFIndividual annotationIndividual);

	protected String generateOWLName(OWLIndividual annotationInd, OWLIndividual resultInd){
		return "Context_"+annotationInd.getLocalName();
	}
	
	protected String generateRDFName(RDFIndividual annotationInd){
		return "Context_"+annotationInd.getLocalName();
	}
	
	/**
	 * Constructs a <code>String</code> with all attributes in name = value format.
	 * @return a <code>String</code> representation of this object.
	 */
	public String toString(){
	    StringBuffer retValue = new StringBuffer();
	    retValue.append(this.contextName).append("(").append(isDirect).append(")");
	    return retValue.toString();
	}	
}
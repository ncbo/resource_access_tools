package org.ncbo.resource_access_tools.common.beans;

import org.ncbo.resource_access_tools.common.service.BioPortalAnnotation;
import org.ncbo.resource_access_tools.common.service.NcboObsOntology;

import edu.stanford.smi.protegex.owl.model.OWLIndividual;
import edu.stanford.smi.protegex.owl.model.RDFIndividual;

/**
 * @author Clemetn Jonquet
 * @version 1.0
 * @created 11-Feb-2008 2:42:12 PM
 */
public class ReportedContextBean extends ContextBean {

	public static final String REPORTED_CTX = "REPORTED";
	public static final int RDA_WEIGHT = 8;
	
	public ReportedContextBean(String contextName) {
		super(contextName, true);
	}
	
	@Override
	public String getOtherContextInformation() {
		return "";
	}

	/**
	 * Returns a OWL individual for that object and creates the necessary other individuals, in the given ontology. 
	 */
	@Override
	public OWLIndividual getOWLIndividual(NcboObsOntology obsOntology, OWLIndividual annotationInd, OWLIndividual resultInd){

		// creates the class individual
		OWLIndividual individual = obsOntology.owlModel.getOWLNamedClass(NcboObsOntology.OWL_REPORTED_CONTEXT).createOWLIndividual(this.generateOWLName(annotationInd, resultInd));

		// assigns the individual properties
		individual.addPropertyValue(obsOntology.owlModel.getOWLObjectProperty(NcboObsOntology.OWL_HAS_ANNOTATION), annotationInd);
		this.assignPropertyValue(individual, obsOntology);
		
		return individual;
	}
	
	/**
	 * Returns a RDF individual for that object and creates the necessary other individuals, in the given ontology. 
	 */
	@Override
	public RDFIndividual getRDFIndividual(BioPortalAnnotation bioPortalAnnotation, RDFIndividual annotationIndividual) {
		// creates the class individual
		RDFIndividual individual = bioPortalAnnotation.owlModel.getRDFSNamedClass(BioPortalAnnotation.RDF_REPORTED_CONTEXT).createRDFIndividual(this.generateRDFName(annotationIndividual));
		// assigns the individual properties
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
	        .append("]");
	    return retValue.toString();
	} 
}

package org.ncbo.resource_access_tools.common.beans;

import org.ncbo.resource_access_tools.common.service.NcboObsOntology;

import edu.stanford.smi.protegex.owl.model.OWLIndividual;

/**
 * @author jonquet
 * @version 1.0
 * @created 24-Sep-2008 2:42:12 PM
 */
public class StatisticsBean {

	private String contextName;
	private int annotationCount;
	
	public StatisticsBean(String contextName, int annotationCount) {
		super();
		this.contextName = contextName;
		this.annotationCount = annotationCount;
	}

	public String getContextName() {
		return contextName;
	}

	public int getAnnotationCount() {
		return annotationCount;
	}
	
	public void addAnnotationCount(int toAddAnnotations) {
		this.annotationCount = this.annotationCount + toAddAnnotations;
	}

	/**
	 * Returns a OWL individual for that object and creates the necessary other individuals, in the given ontology. 
	 */
	public OWLIndividual getOWLIndividual(NcboObsOntology obsOntology, OWLIndividual resultInd){

		// creates the class individual
		OWLIndividual individual = obsOntology.owlModel.getOWLNamedClass(NcboObsOntology.OWL_STATISTICS).createOWLIndividual(resultInd.getLocalName() +"_"+ this.contextName);
		
		// assigns the individual properties
		individual.addPropertyValue(obsOntology.owlModel.getOWLObjectProperty(NcboObsOntology.OWL_HAS_RESULT), resultInd);
		
		individual.addPropertyValue(obsOntology.owlModel.getOWLDatatypeProperty(NcboObsOntology.OWL_CONTEXT_NAME), this.contextName);
		individual.addPropertyValue(obsOntology.owlModel.getOWLDatatypeProperty(NcboObsOntology.OWL_NB_ANNOTATION),Integer.valueOf(this.annotationCount));
		
		return individual;
	}
	
	/**
	 * Constructs a <code>String</code> with all attributes in name = value format.
	 * @return a <code>String</code> representation of this object.
	 */
	public String toString(){
	    final String TAB = ", ";
	    StringBuffer retValue = new StringBuffer();
	    retValue.append("(").append(this.contextName).append(TAB)
	    	.append(this.annotationCount).append(") ");
	    return retValue.toString();
	}
		
}
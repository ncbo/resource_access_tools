package org.ncbo.resource_access_tools.common.beans;

import org.ncbo.resource_access_tools.common.service.NcboObsOntology;

import edu.stanford.smi.protegex.owl.model.OWLIndividual;

/**
 * This class is a JavaBean representation of an ontology within OBS used during a OBA/OBR request.
 * These ontologies have 2 properties:
 *  * <ul>
 * <li> a number of annotations in the reslut that have been done with this ontology,</li>
 * <li> a score, that represent the adequacy of the ontology to annotate the content of the result.</li>
 * </ul>
 *    
 * @author Clement Jonquet
 * @version 1.0
 * @created 05-Nov-2008 2:42:11 PM
 */
public class OntologyUsedBean extends OntologyBean {

	private int annotationCount;
	private float score;
	
	public OntologyUsedBean(String localOntologyId, String ontologyName, String ontologyVersion, String virtualOntologyId, int annotationCount, float score) {
		super(localOntologyId, ontologyName, ontologyVersion, virtualOntologyId);
		this.annotationCount = annotationCount;
		this.score = score;
	}

	public OntologyUsedBean(OntologyBean ontology, int annotationCount, float score){
		super(ontology.getLocalOntologyId(), ontology.getOntologyName(), ontology.getOntologyVersion(), ontology.getVirtualOntologyId());
		this.annotationCount = annotationCount;
		this.score = score;	
	}
	
	public int getAnnotationCount() {
		return annotationCount;
	}

	public float getScore() {
		return score;
	}
	
	public void setAnnotationCount(int annotationCount) {
		this.annotationCount = annotationCount;
	}

	public void setScore(float score) {
		this.score = score;
	}

	/**
	 * Returns a OWL individual for that object and creates the necessary other individuals, in the given ontology. 
	 */
	public OWLIndividual getOWLIndividual(NcboObsOntology obsOntology, OWLIndividual resultInd){

		// creates the class individual
		OWLIndividual individual = obsOntology.owlModel.getOWLNamedClass(NcboObsOntology.OWL_ONTOLOGY_USED).createOWLIndividual(resultInd.getLocalName()+ "_" +super.getLocalOntologyId());

		// assigns the individual properties (super class)
		this.assignPropertyValue(individual, obsOntology);
		
		// assigns the individual properties
		individual.addPropertyValue(obsOntology.owlModel.getOWLObjectProperty(NcboObsOntology.OWL_HAS_RESULT), resultInd);
		individual.addPropertyValue(obsOntology.owlModel.getOWLDatatypeProperty(NcboObsOntology.OWL_NB_ANNOTATION), Integer.valueOf(this.annotationCount));
		individual.addPropertyValue(obsOntology.owlModel.getOWLDatatypeProperty(NcboObsOntology.OWL_SCORE), Float.valueOf(this.score));
		
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
		.append(this.getOntologyName()).append(TAB)
		.append(this.annotationCount).append(TAB)
		.append(this.score).append(TAB)
		.append("(")
		.append(this.getLocalOntologyId()).append(TAB)
		.append(this.getOntologyVersion()).append(")")
		.append("]");
		return retValue.toString();
	}

}

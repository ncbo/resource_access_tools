package org.ncbo.resource_access_tools.common.beans;

import java.util.Iterator;

import org.ncbo.resource_access_tools.common.service.BioPortalAnnotation;
import org.ncbo.resource_access_tools.common.service.NcboObsOntology;

import edu.stanford.smi.protegex.owl.model.OWLIndividual;
import edu.stanford.smi.protegex.owl.model.RDFIndividual;

/**
 * This class is a JavaBean representation of an annotation within OBS.
 * An annotation involves a unique concept and a unique context.
 * An annotation has a score that represents the accuracy of the annotation according to a given method. 
 * If the scoring method has not be applied on a set of annotations, then  this score is by default -1. 
 *  
 * Annotation are constructed with score=-1 until the scoring algorithm is performed on an annotation set. 
 *    
 * @author Clement Jonquet
 * @version 1.0
 * @created 24-Sep-2008 2:42:11 PM
 */
public class AnnotationBean {

	private float score;
	private ConceptBean concept;
	private ContextBean context;
	
	// used to uniquely indentify annotations and give them a unique OWL name
	private static int count;
	
	public AnnotationBean(ConceptBean concept, ContextBean context) {
		super();
		this.score = -1;
		this.concept = concept;
		this.context = context;
	}

	public float getScore() {
		return score;
	}

	public void setScore(float score) {
		this.score = score;
	}

	public ConceptBean getConcept() {
		return concept;
	}

	public ContextBean getContext() {
		return context;
	}

	public String toTabDelimited(){
		StringBuffer lineb = new StringBuffer();
		lineb.append(this.getScore());
		
		if(this.getConcept() != null){
			lineb.append("\t");
			lineb.append(this.getConcept().getLocalConceptId());
			lineb.append("\t");
			lineb.append(this.getConcept().getPreferredName());
			lineb.append("\t");
			for(Iterator<String>it = this.getConcept().getSynonyms().iterator(); it.hasNext();){
				lineb.append(it.next());
				if(it.hasNext()){
					lineb.append(" /// ");
				}
			}
			lineb.append("\t");
			for(Iterator<String>it = this.getConcept().getLocalSemanticTypeIds().iterator(); it.hasNext();){
				lineb.append(it.next());
				if(it.hasNext()){
					lineb.append(" /// ");
				}
			}
		}	
		
		lineb.append("\t");
		lineb.append(this.getContext().getContextName());
		lineb.append("\t");
		lineb.append(this.getContext().isDirect());
		lineb.append("\t");
		lineb.append(this.getContext().getOtherContextInformation());
		return lineb.toString();
	}
	
	/**
	 * Returns a OWL individual for that object and creates the necessary other individuals, in the given ontology. 
	 */
	public OWLIndividual getOWLIndividual(NcboObsOntology obsOntology, OWLIndividual resultInd){

		// creates the class individual
		OWLIndividual individual = obsOntology.owlModel.getOWLNamedClass(NcboObsOntology.OWL_ANNOTATION).createOWLIndividual(NcboObsOntology.OWL_ANNOTATION +"_"+ resultInd.getLocalName()+"_"+count());

		// assigns the individual properties
		individual.addPropertyValue(obsOntology.owlModel.getOWLObjectProperty(NcboObsOntology.OWL_HAS_RESULT), resultInd);
		this.assignPropertyValue(individual, resultInd, obsOntology);
		
		return individual;
	}
	
	protected void assignPropertyValue(OWLIndividual individual, OWLIndividual resultInd, NcboObsOntology obsOntology){
		individual.addPropertyValue(obsOntology.owlModel.getOWLDatatypeProperty(NcboObsOntology.OWL_SCORE), Float.valueOf(this.score));
		if(this.concept != null){
			individual.addPropertyValue(obsOntology.owlModel.getOWLObjectProperty(NcboObsOntology.OWL_HAS_CONCEPT), this.concept.getOWLIndividual(obsOntology, individual));
		}		
		if(this.context != null){
			individual.addPropertyValue(obsOntology.owlModel.getOWLObjectProperty(NcboObsOntology.OWL_HAS_CONTEXT), this.context.getOWLIndividual(obsOntology, individual, resultInd));
		}
	} 
	
	/**
	 * Adding bean properties to RDF individuals
	 * 
	 * @param individual
	 * @param bioPortalAnnotation
	 */
	protected void assignPropertyValue(RDFIndividual individual, BioPortalAnnotation bioPortalAnnotation){
		individual.addPropertyValue(bioPortalAnnotation.owlModel.getRDFProperty(NcboObsOntology.OWL_SCORE), Float.valueOf(this.score));
		if(this.concept != null && this.concept.getLocalConceptId()!= null){
			individual.addPropertyValue(bioPortalAnnotation.owlModel.getRDFProperty(BioPortalAnnotation.RDF_ANNOTATION_CONCEPT), bioPortalAnnotation.createAnnotatedConceptURL(this.concept.getLocalConceptId()));
		}		
		if(this.context != null){
	    	individual.addPropertyValue(bioPortalAnnotation.owlModel.getRDFProperty(BioPortalAnnotation.RDF_ANNOTATION_CONTEXT), this.context.getRDFIndividual(bioPortalAnnotation, individual));
		} 
	} 
	
	protected static int count(){
		count++;
		return count;
	}
	
	/**
	 * Constructs a <code>String</code> with all attributes in name = value format.
	 * @return a <code>String</code> representation of this object.
	 */
	public String toString(){
	    final String TAB = "\r\n";
	    StringBuffer retValue = new StringBuffer();
	    retValue.append("annotation [ ").append(TAB)
	        //.append(super.toString()).append(TAB)
	        .append("\t\tscore = ").append(this.score).append(TAB)
	        .append("\t\tconcept = ").append(this.concept).append(TAB)
	        .append("\t\tcontext = ").append(this.context).append(TAB)
	        .append("]");
	    return retValue.toString();
	}
	
}
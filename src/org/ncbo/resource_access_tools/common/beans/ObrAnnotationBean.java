package org.ncbo.resource_access_tools.common.beans;

import java.util.Iterator;

import org.ncbo.resource_access_tools.common.service.BioPortalAnnotation;
import org.ncbo.resource_access_tools.common.service.NcboObsOntology;

import edu.stanford.smi.protegex.owl.model.OWLIndividual;
import edu.stanford.smi.protegex.owl.model.RDFIndividual;

/**
 * @author Clement Jonquet
 * @version 1.0
 * @created 24-Sep-2008 2:42:12 PM
 */
public class ObrAnnotationBean extends AnnotationBean {

	private String localElementId;
	
	public ObrAnnotationBean(ConceptBean concept, ContextBean context, String localElementId, float score) {
		super(concept, context);
		this.setScore(score);
		this.localElementId = localElementId;
	}

	public String getLocalElementId() {
		return localElementId;
	}

	/**
	 * Returns a OWL individual for that object and creates the necessary other individuals, in the given ontology. 
	 */
	public OWLIndividual getOWLIndividual(NcboObsOntology obsOntology, OWLIndividual resultInd){

		// creates the class individual
		OWLIndividual individual = obsOntology.owlModel.getOWLNamedClass(NcboObsOntology.OWL_OBR_ANNOTATION).createOWLIndividual(resultInd.getName()+"_"+count());

		// assigns the individual properties
		individual.addPropertyValue(obsOntology.owlModel.getOWLObjectProperty(NcboObsOntology.OWL_HAS_RESULT), resultInd);
		this.assignPropertyValue(individual, resultInd, obsOntology);
		individual.addPropertyValue(obsOntology.owlModel.getOWLDatatypeProperty(NcboObsOntology.OWL_LOCAL_ELEMENTID), this.localElementId);
		
		return individual;
	}
	
	/**
	 * Returns a RDF individual for that object and creates the necessary other individuals, in the given ontology. 
	 */
	public RDFIndividual getRDFIndividual(BioPortalAnnotation bioPortalAnnotation, String resourceId) {
		// creates the class individual
		RDFIndividual individual =  bioPortalAnnotation.owlModel.getRDFSNamedClass(BioPortalAnnotation.RDF_RESOURCE_INDEX_ANNOTATION).createRDFIndividual("RI_Annotation_" + count());
		// assigns the individual properties
		individual.addPropertyValue(bioPortalAnnotation.owlModel.getRDFProperty(BioPortalAnnotation.RDF_ANNOTATED_ELEMENT), bioPortalAnnotation.createAnnotatedElementURL(resourceId, this.localElementId));
		this.assignPropertyValue(individual, bioPortalAnnotation);
		
		return individual;
	}
	
	/**
	 * Constructs a <code>String</code> with all attributes
	 * in name = value format.
	 *
	 * @return a <code>String</code> representation 
	 * of this object.
	 */
	public String toString() {
	    final String TAB = "\t\n";
	    StringBuffer retValue = new StringBuffer();
	    retValue.append(super.toString()).append("\t\t-->")
	        .append("localElementId = ").append(this.localElementId).append(TAB);
	    return retValue.toString();
	}
	
	/**
	 * Tab delimited representation of bean.
	 * 
	 */
	public String toTabDelimited(){
		StringBuffer lineb = new StringBuffer();
		lineb.append(this.getLocalElementId());
		lineb.append("\t");
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
		
		
		if(this.getContext()!= null){
			lineb.append("\t");
			lineb.append(this.getContext().getContextName());
			lineb.append("\t");
			lineb.append(this.getContext().isDirect());
			lineb.append("\t");
			lineb.append(this.getContext().getOtherContextInformation());
		} 
		
		return lineb.toString();
	}
	
}
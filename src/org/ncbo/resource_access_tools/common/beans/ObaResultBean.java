package org.ncbo.resource_access_tools.common.beans;

import java.util.ArrayList;
import java.util.HashSet;

import org.ncbo.resource_access_tools.common.service.NcboObsOntology;

import edu.stanford.smi.protegex.owl.model.OWLIndividual;

/**
 * This class is a JavaBean representation of an OBA result.
 * This is the main object returned by a call to OBA.
 * 
 * A ObaResultBean has the following fields:
 * <li>
 * 	<ul> It is is associated to zero or several {@link AnnotationBean} which represent the annotation 
 * 	done for the piece of text.</ul>
 * 	<ul> A string that represents the text that has been annotated.</ul>
 * 	<ul> A string that represents the version of OBA.</ul>
 * </li>
 * 
 * @author Clement Jonquet
 * @version 1.0
 * @created 24-Sep-2008 2:42:12 PM
 */
public class ObaResultBean extends ResultBean {

	public static final String OBA_VERSION = "OBA_v1.1";
	private String text;
	private ArrayList<AnnotationBean> annotations;
	private ArrayList<OntologyUsedBean> ontologies;
	
	public ObaResultBean(String resultId, DictionaryBean dictionary, HashSet<StatisticsBean> statistics, ParametersBean parameters,
			ArrayList<OntologyUsedBean> ontologies, String text, ArrayList<AnnotationBean> annotations) {
		super(resultId, dictionary, statistics, parameters);
		this.text = text;
		this.annotations = annotations;
		this.ontologies = ontologies;
	}

	public String getText() {
		return text;
	}

	public ArrayList<AnnotationBean> getAnnotations() {
		return annotations;
	}
	
	public ArrayList<OntologyUsedBean> getOntologies() {
		return ontologies;
	}

	/** The corresponding format is:
	 * score \t localConceptId \t preferredName \t synonyms (separated by ' /// ') \t localSemanticTypeId (separated by ' /// ')
	 * 	\t contextName \t isDirect \t other context information (e.g., childConceptId, mappedConceptId, level, mappingType) (separated by ' /// ')
	 * (note end of line is done with \r\n characters).
	 */
	public String toTabDelimited(){
		StringBuffer lineb = new StringBuffer();
		// for each annotations
		for(AnnotationBean annotation: this.annotations){
			lineb.append(annotation.toTabDelimited());
			lineb.append("\r\n");
		}
		return lineb.toString();
	}

	public String toOWL(NcboObsOntology obsOntology){
		return super.toOWL(obsOntology);
	}
	
	/**
	 * Returns a OWL individual for that object and creates the necessary other individuals, in the given ontology. 
	 */
	@Override
	public OWLIndividual getOWLIndividual(NcboObsOntology obsOntology){

		// creates the class individual
		OWLIndividual individual = obsOntology.owlModel.getOWLNamedClass(NcboObsOntology.OWL_OBA_RESULT).createOWLIndividual(this.getResultId());

		// assigns the individual properties (super class)
		this.assignPropertyValue(individual, obsOntology);
		
		// assigns the individual properties
		individual.addPropertyValue(obsOntology.owlModel.getOWLDatatypeProperty(NcboObsOntology.OWL_TEXT_TO_ANNOTATE), this.text);
		for (AnnotationBean annotation: this.annotations){
			individual.addPropertyValue(obsOntology.owlModel.getOWLObjectProperty(NcboObsOntology.OWL_HAS_ANNOTATION), annotation.getOWLIndividual(obsOntology, individual));
		}
		for (OntologyUsedBean ontology: this.ontologies){
			individual.addPropertyValue(obsOntology.owlModel.getOWLObjectProperty(NcboObsOntology.OWL_HAS_ONTOLOGY_USED), ontology.getOWLIndividual(obsOntology, individual));
		}
		
		return individual;
	}
	
	/**
	 * Constructs a <code>String</code> with all attributes in name = value format.
	 * @return a <code>String</code> representation of this object.
	 */
	public String toString(){
	    final String TAB = "\r\n";
	    StringBuffer retValue = new StringBuffer();
	    retValue.append("ObaResultBean [ ").append(TAB)
	        .append("\ttext = ").append(this.text).append(TAB)
	        .append(super.toString()).append(TAB)
     	    .append("\tontologies = ").append(this.ontologies).append(TAB)
	        .append("\tannotations = ").append(this.annotations).append(TAB)
	        .append("]");
	    return retValue.toString();
	}
	
	/**
	 * Returns a String that contains only the ontology information of the result.
	 */
	public String toStringUsedOntologies(){
		final String TAB = "\r\n";
	    StringBuffer retValue = new StringBuffer();
	    retValue.append("ObaResultBean (used ontologies only) [ ").append(TAB)
	        .append("\ttext = ").append(this.text).append(TAB)
	        .append("\tresultId = ").append(this.getResultId()).append(TAB)
	        .append("\tdictionary = ").append(this.getDictionary()).append(TAB)
     	    .append("\tontologies = ").append(TAB);
	    	for(OntologyUsedBean ontology: this.ontologies){
	    		retValue.append("[")
	    		.append(ontology.getOntologyName()).append("(")
	    		.append("localOntologyId: ").append(ontology.getLocalOntologyId()).append(", ")
	    		.append("version: ").append(ontology.getOntologyVersion()).append(")").append(TAB)
	    		.append("\t annotations:\t").append(ontology.getAnnotationCount()).append(TAB)
	    		.append("\t score:\t\t").append(ontology.getScore()).append(TAB)    		
	    		.append("]").append(TAB);
	    	}
	    	retValue.append("]");
	    return retValue.toString();
	}
		
}
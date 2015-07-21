package org.ncbo.resource_access_tools.common.beans;

import org.ncbo.resource_access_tools.common.service.BioPortalAnnotation;
import org.ncbo.resource_access_tools.common.service.NcboObsOntology;

import edu.stanford.smi.protegex.owl.model.OWLIndividual;
import edu.stanford.smi.protegex.owl.model.RDFIndividual;


/**
 * @author Clemetn Jonquet
 * @version 1.0
 * @created 24-Sep-2008 2:42:12 PM
 */
public class MgrepContextBean extends ContextBean {

	public static final String MGREP_CTX = "MGREP";
	public static final int PDA_WEIGHT = 10;
	public static final int SDA_WEIGHT = 8;
	
	// termId added December 5th 2008, to allow internal users of the OBA service to map back to a unique termId in the OBS_TT table
	private int termId;
	private String termName;
	private int from;
	private int to;
	
	public MgrepContextBean(String contextName, int termId, String termName, int from, int to) {
		super(contextName, true);
		this.termId = termId;
		this.termName = termName;
		this.from = from;
		this.to = to;
	}
	
	public int getTermId() {
		return termId;
	}

	public String getTermName() {
		return termName;
	}

	public int getFrom() {
		return from;
	}

	public int getTo() {
		return to;
	}

	public String getOtherContextInformation(){
		return this.termName + "///" + this.from + "///" + this.to;
	}

	/**
	 * Returns a OWL individual for that object and creates the necessary other individuals, in the given ontology. 
	 */
	@Override
	public OWLIndividual getOWLIndividual(NcboObsOntology obsOntology, OWLIndividual annotationInd, OWLIndividual resultInd){

		// creates the class individual
		OWLIndividual individual = obsOntology.owlModel.getOWLNamedClass(NcboObsOntology.OWL_MGREP_CONTEXT).createOWLIndividual(this.generateOWLName(annotationInd, resultInd));

		// assigns the individual properties
		individual.addPropertyValue(obsOntology.owlModel.getOWLObjectProperty(NcboObsOntology.OWL_HAS_ANNOTATION), annotationInd);
		individual.addPropertyValue(obsOntology.owlModel.getOWLDatatypeProperty(NcboObsOntology.OWL_TERMID), obsOntology.convertToPositiveInteger(this.termId));
		individual.addPropertyValue(obsOntology.owlModel.getOWLDatatypeProperty(NcboObsOntology.OWL_TERM_NAME), this.termName);
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
		RDFIndividual individual = bioPortalAnnotation.owlModel.getRDFSNamedClass(BioPortalAnnotation.RDF_MGREP_CONTEXT).createRDFIndividual(this.generateRDFName(annotationIndividual));

		// assigns the individual properties
		individual.addPropertyValue(bioPortalAnnotation.owlModel.getRDFProperty(BioPortalAnnotation.RDF_TERMID), Integer.valueOf(this.termId));
		individual.addPropertyValue(bioPortalAnnotation.owlModel.getRDFProperty(BioPortalAnnotation.RDF_TERM_NAME), this.termName);
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
	        .append("termName = ").append(this.termName).append(TAB)
	        .append("from = ").append(this.from).append(TAB)
	        .append("to = ").append(this.to)
	        .append("]");
	    return retValue.toString();
	} 
}
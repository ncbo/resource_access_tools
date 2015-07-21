package org.ncbo.resource_access_tools.common.beans;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.HashSet;

import org.ncbo.resource_access_tools.common.service.NcboObsOntology;

import obs.ontologyAccess.BioPortalAccessTool;
import edu.stanford.smi.protegex.owl.model.OWLIndividual;

/**
* This class defines the bean that defines a concept within the OBS framework.
*  
* @author Clement Jonquet
* @version 1.0
* @created 18-Sept-2008
*
*/
public class ConceptBean {

	private String localConceptId;
	private String preferredName;
	private HashSet<String> synonyms; // synonyms should be cleaned to be used in the annotation workflow
	private boolean isRoot;
	private String localOntologyId;
	private HashSet<String> localSemanticTypeIds;
	
	public ConceptBean(String localConceptId, String preferredName, HashSet<String> synonyms, boolean isRoot, String localOntologyId, HashSet<String> localSemanticTypeIds) {
		super();
		this.localConceptId = localConceptId;
		this.preferredName = preferredName;
		this.synonyms = cleanSynonyms(synonyms);
		this.isRoot = isRoot;
		this.localOntologyId = localOntologyId;
		this.localSemanticTypeIds = localSemanticTypeIds;
	}
	
	/**
	 * Returns a concept bean (OBS framework) with a bean from the ontologyAccess framework and
	 * the complementary information.
	 */
	public ConceptBean(String localOntologyId, boolean isRoot, obs.ontologyAccess.bean.BioPortalFullConceptBean conceptBean){
		super();
		this.localConceptId = BioPortalAccessTool.createLocalConceptID(localOntologyId, conceptBean.getId());
		this.preferredName = conceptBean.getLabel();
		this.synonyms = new HashSet<String>();
		this.synonyms.addAll(conceptBean.getExactSynonyms());
		this.synonyms.addAll(conceptBean.getNarrowSynonyms());
		this.synonyms.addAll(conceptBean.getBroadSynonyms());
		this.synonyms.addAll(conceptBean.getRelatedSynonyms());
		this.synonyms.addAll(conceptBean.getBpSynonyms());
		this.synonyms = cleanSynonyms(this.synonyms);
		this.isRoot = isRoot;
		this.localOntologyId = localOntologyId;
		this.localSemanticTypeIds = new HashSet<String>(0);;
	}
	
	public String getLocalConceptId() {
		return localConceptId;
	}
	
	public String getPreferredName() {
		return preferredName;
	}
	
	public HashSet<String> getSynonyms() {
		return synonyms;
	}
	
	public boolean isRoot() {
		return isRoot;
	}
	
	public String getLocalOntologyId() {
		return localOntologyId;
	}
	
	public HashSet<String> getLocalSemanticTypeIds() {
		return localSemanticTypeIds;
	}

	public ConceptEntry getConceptEntry(){
		return new ConceptEntry(this.localConceptId, this.localOntologyId, this.isRoot);
	}
	
	/**
	 * Returns a OWL individual for that object and creates the necessary other individuals, in the given ontology. 
	 */
	public OWLIndividual getOWLIndividual(NcboObsOntology obsOntology, OWLIndividual annotationInd){

		// creates the class individual
		String conceptOwlName;
		boolean alreadyExist = true;
		try {
			conceptOwlName = URLEncoder.encode(this.localConceptId, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			conceptOwlName = this.localConceptId;
			e.printStackTrace();
		}
		
		OWLIndividual individual = obsOntology.owlModel.getOWLIndividual(conceptOwlName);
		if(individual == null){
			individual = obsOntology.owlModel.getOWLNamedClass(NcboObsOntology.OWL_CONCEPT).createOWLIndividual(conceptOwlName);
			alreadyExist = false;
		}
		
		// assigns the individual properties all the time 
		individual.addPropertyValue(obsOntology.owlModel.getOWLObjectProperty(NcboObsOntology.OWL_HAS_ANNOTATION), annotationInd);
		// assigns the individual properties only the first time
		if(!alreadyExist){
			individual.addPropertyValue(obsOntology.owlModel.getOWLDatatypeProperty(NcboObsOntology.OWL_LOCAL_CONCEPTID), this.localConceptId);
			individual.addPropertyValue(obsOntology.owlModel.getOWLDatatypeProperty(NcboObsOntology.OWL_PREFERRED_NAME), this.preferredName);
			for(String synonym: this.synonyms){
				individual.addPropertyValue(obsOntology.owlModel.getOWLDatatypeProperty(NcboObsOntology.OWL_SYNONYM), synonym);
			}
			individual.addPropertyValue(obsOntology.owlModel.getOWLDatatypeProperty(NcboObsOntology.OWL_IS_ROOT), Boolean.valueOf(this.isRoot));
			individual.addPropertyValue(obsOntology.owlModel.getOWLDatatypeProperty(NcboObsOntology.OWL_LOCAL_ONTOLOGYID), this.localOntologyId);
			for(String localSemanticTypeId: this.localSemanticTypeIds){
				individual.addPropertyValue(obsOntology.owlModel.getOWLDatatypeProperty(NcboObsOntology.OWL_LOCAL_STT), localSemanticTypeId);
			}
		}
		
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
	        .append(this.localConceptId).append(TAB)
	        .append(this.preferredName).append(TAB)
	        .append(this.synonyms).append(TAB)
	        .append("isRoot = ").append(this.isRoot).append(TAB)
	        .append(this.localOntologyId).append(TAB)
	        .append(this.localSemanticTypeIds)
	        .append("]");
	    return retValue.toString();
	}
	
	private static HashSet<String> cleanSynonyms(HashSet<String> synonyms){
		HashSet<String> cleanSynonyms = new HashSet<String>();
		// for each synonyms remove the garbage to be able to be used by the concept recognition tool
		for(String synonym: synonyms){
			// if the string is between ", then remove them
			if(synonym.matches("\".+\"")){
				cleanSynonyms.add(synonym.substring(1, synonym.length()-1));
			}
			else{
				cleanSynonyms.add(synonym);
			}
		}
		return cleanSynonyms;
	}
	
}

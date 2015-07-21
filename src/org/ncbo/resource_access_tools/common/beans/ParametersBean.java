package org.ncbo.resource_access_tools.common.beans;

import java.util.HashSet;

import org.ncbo.resource_access_tools.common.service.NcboObsOntology;
import org.ncbo.resource_access_tools.common.utils.Utilities;

import obaclient.OBAServiceStub.OpenBiomedicalAnnotator;
import edu.stanford.smi.protegex.owl.model.OWLIndividual;
 

/**
* This class implements the parameters for OBA/OBR queries.
*
* @author Clement Jonquet
* @version OBS_v1
* @created 24-Sep-2008
*/
public class ParametersBean {
	//
	private String email;
	private String applicationid;	
	private String format;
	
	// Mgrep parameters
	private boolean longestOnly;
	private boolean wholeWordOnly;
	private HashSet<String> stopWords;
	
	private boolean filterNumber;
	private int minTermSize;
	
	// OBS general parameters
	private boolean scored;
	private boolean withSynonyms;
	private HashSet<String> ontologiesToKeepInResult;
	private boolean isVirtualOntologyId;
	private HashSet<String> semanticTypes;
	
	private boolean withDefaultStopWords;
	private HashSet<String> additionalStopWords;	
	private boolean isStopWordsCaseSenstive;	 
	
	// Semantic expansion parameters	 
	private int levelMax;
	private HashSet<String> mappingTypes;
		
	/**
	 * Constructs a new set of parameters to be used with {@link OpenBiomedicalAnnotator}}.
	 *  
	 * @param longestOnly 
	 * Mgrep parameter. If true, Mgrep will return the longest match only.
	 * @param wholeWordOnly
	 * Mgrep parameter. If true, Mgrep will return whole word match only.
	 * @param stopWords
	 * Workflow paramter. Set of words to ignore in the workflow.
	 * @param minTermSize
	 * Workflow paramter. Minimum size (or equal) that a term must have to be matched.
	 * @param scored
	 * If true, OBA will score the annotations before returning them.
	 * @param withSynonyms
	 * If false, OBA will ignore direct annotations done with synonyms.
	 * @param localOntologyIds
	 * Set of ontology to be restricted to. If null, all ontologies are used.
	 * @param semanticTypes
	 * Set of semantic type to be restricted to. If null, all semantic type are used.
	 * @param levelMin
	 * Minimum parent level for the is_a relations transitive closure expansion step. 
	 * @param levelMax
	 * Maximum parent level for the is_a relations transitive closure expansion step. 
	 * 0 is equivalent to no expansion
	 * @param mappingTypes
	 * Set of mapping relation names used for the mapping expansion step. If null, all relation are used. 
	 */
	public ParametersBean(boolean longestOnly, boolean wholeWordOnly, String[] stopWords, int minTermSize, boolean scored, boolean withSynonyms, String[] ontologiesToKeepInResult,
			boolean isVirtualOntologyId, String[] semanticTypes,  int levelMax, String[] mappingTypes, String email, String applicationid, String format) {
		super();
		this.longestOnly = longestOnly;
		this.wholeWordOnly = wholeWordOnly;
		this.stopWords = Utilities.arrayToHashSet(stopWords);
		this.minTermSize = minTermSize;
		this.scored = scored;
		this.withSynonyms = withSynonyms;
		this.ontologiesToKeepInResult = Utilities.arrayToHashSet(ontologiesToKeepInResult);
		this.isVirtualOntologyId =isVirtualOntologyId;
		this.semanticTypes = Utilities.arrayToHashSet(semanticTypes);		 
		this.levelMax = levelMax;
		this.mappingTypes = Utilities.arrayToHashSet(mappingTypes);
		this.additionalStopWords = new HashSet<String>();
		this.withDefaultStopWords = true;			
		this.email = email;
		this.applicationid = applicationid;
		this.format = format;
	} 

	/**
	 * Constructs a new set of parameters to be used with {@link OpenBiomedicalAnnotator}}.
	 *  
	 * @param longestOnly 
	 * Mgrep parameter. If true, Mgrep will return the longest match only.
	 * @param wholeWordOnly
	 * Mgrep parameter. If true, Mgrep will return whole word match only.
	 * @param stopWords
	 * Workflow paramter. Set of words to ignore in the workflow.
	 * @param minTermSize
	 * Workflow paramter. Minimum size (or equal) that a term must have to be matched.
	 * @param scored
	 * If true, OBA will score the annotations before returning them.
	 * @param withSynonyms
	 * If false, OBA will ignore direct annotations done with synonyms.
	 * @param localOntologyIds
	 * Set of ontology to be restricted to. If null, all ontologies are used.
	 * @param semanticTypes
	 * Set of semantic type to be restricted to. If null, all semantic type are used.
	 * @param levelMin
	 * Minimum parent level for the is_a relations transitive closure expansion step. 
	 * @param levelMax
	 * Maximum parent level for the is_a relations transitive closure expansion step. 
	 * 0 is equivalent to no expansion
	 * @param mappingTypes
	 * Set of mapping relation names used for the mapping expansion step. If null, all relation are used. 
	 * 
	 * @param additionalStopWords	 
	 * @param isStopWordsCaseSenstive
	 * 
	 */
	public ParametersBean(boolean longestOnly, boolean wholeWordOnly, String[] stopWords, boolean filterNumber, int minTermSize, boolean scored, boolean withSynonyms, String[] ontologiesToKeepInResult,
			boolean isVirtualOntologyId, String[] semanticTypes, int levelMax, String[] mappingTypes, String[] additionalStopWords, boolean isStopWordsCaseSenstive, String email, String applicationid, String format) {
		super();
		this.longestOnly = longestOnly;
		this.wholeWordOnly = wholeWordOnly;
		this.stopWords = Utilities.arrayToHashSet(stopWords);
		this.filterNumber = filterNumber;
		this.minTermSize = minTermSize;
		this.scored = scored;
		this.withSynonyms = withSynonyms;
		this.ontologiesToKeepInResult = Utilities.arrayToHashSet(ontologiesToKeepInResult);
		this.isVirtualOntologyId = isVirtualOntologyId;
		this.semanticTypes = Utilities.arrayToHashSet(semanticTypes);		 
		this.levelMax = levelMax;
		this.mappingTypes = Utilities.arrayToHashSet(mappingTypes);
		this.additionalStopWords = Utilities.arrayToHashSet(additionalStopWords);
		this.isStopWordsCaseSenstive = isStopWordsCaseSenstive;
		this.withDefaultStopWords = true;	
		this.format = format;
		this.email = email;
		this.applicationid = applicationid;
	}

	public String getEmail() {
		return email;
	}

	public String getApplicationid() {
		return applicationid;
	}

	public String getFormat() {
		return format;
	}

	public boolean isLongestOnly() {
		return longestOnly;
	}

	public boolean isWholeWordOnly() {
		return wholeWordOnly;
	}
	
	public int getMinTermSize() {
		return minTermSize;
	}

	public HashSet<String> getStopWords() {
		return stopWords;
	}
	
	public boolean isFilterNumber() {
		return filterNumber;
	}

	public boolean isScored() {
		return scored;
	}

	public boolean isWithSynonyms() {
		return withSynonyms;
	} 
 
	public HashSet<String> getOntologiesToKeepInResult() {
		return ontologiesToKeepInResult;
	} 
	
	public HashSet<String> getSemanticTypes() {
		return semanticTypes;
	}

	public int getLevelMax() {
		return levelMax;
	}
	
	public HashSet<String> getMappingTypes() {
		return mappingTypes;
	} 
	
	public HashSet<String> getAdditionalStopWords() {
		return additionalStopWords;
	}

	public boolean isStopWordsCaseSenstive() {
		return isStopWordsCaseSenstive;
	}
	
	public boolean isVirtualOntologyId() {
		return isVirtualOntologyId;
	}

	public boolean isWithDefaultStopWords() {
		return withDefaultStopWords;
	}

	/**
	 * Returns a OWL individual for that object and creates the necessary other individuals, in the given ontology. 
	 */
	public OWLIndividual getOWLIndividual(NcboObsOntology obsOntology, OWLIndividual resultInd){

		// creates the class individual
		OWLIndividual individual = obsOntology.owlModel.getOWLNamedClass(NcboObsOntology.OWL_PARAMETERS).createOWLIndividual(NcboObsOntology.OWL_PARAMETERS + "_" +resultInd.getLocalName());
		
		// assigns the individual properties
		individual.addPropertyValue(obsOntology.owlModel.getOWLObjectProperty(NcboObsOntology.OWL_HAS_RESULT), resultInd);
		
		if(this.email != null){
			individual.addPropertyValue(obsOntology.owlModel.getOWLDatatypeProperty(NcboObsOntology.OWL_EMAIL), this.email);
		}		
        if(this.applicationid != null){
        	individual.addPropertyValue(obsOntology.owlModel.getOWLDatatypeProperty(NcboObsOntology.OWL_APPLICATIONID), this.applicationid);
		}        
        if(this.format != null){
        	individual.addPropertyValue(obsOntology.owlModel.getOWLDatatypeProperty(NcboObsOntology.OWL_FORMAT),this.format);
		}
		 				
		individual.addPropertyValue(obsOntology.owlModel.getOWLDatatypeProperty(NcboObsOntology.OWL_LONGEST_ONLY), Boolean.valueOf(this.longestOnly));
		individual.addPropertyValue(obsOntology.owlModel.getOWLDatatypeProperty(NcboObsOntology.OWL_WHOLEWORD_ONLY), Boolean.valueOf(this.wholeWordOnly));
		for(String stopWord: this.stopWords){
			individual.addPropertyValue(obsOntology.owlModel.getOWLDatatypeProperty(NcboObsOntology.OWL_STOP_WORDS), stopWord);
		}
		individual.addPropertyValue(obsOntology.owlModel.getOWLDatatypeProperty(NcboObsOntology.OWL_WITH_DEFAULT_STOP_WORD), Boolean.valueOf(this.withDefaultStopWords));
		individual.addPropertyValue(obsOntology.owlModel.getOWLDatatypeProperty(NcboObsOntology.OWL_MIN_TERM_SIZE), Integer.valueOf(this.minTermSize));
		individual.addPropertyValue(obsOntology.owlModel.getOWLDatatypeProperty(NcboObsOntology.OWL_SCORED), Boolean.valueOf(this.scored));
		individual.addPropertyValue(obsOntology.owlModel.getOWLDatatypeProperty(NcboObsOntology.OWL_WITH_SYN), Boolean.valueOf(this.withSynonyms));
		for (String localOntologyId: this.ontologiesToKeepInResult){
			individual.addPropertyValue(obsOntology.owlModel.getOWLDatatypeProperty(NcboObsOntology.OWL_ONTO_TO_KEEP_IN_RESULT), localOntologyId);
		}
		individual.addPropertyValue(obsOntology.owlModel.getOWLDatatypeProperty(NcboObsOntology.OWL_IS_VIRTUAL_ONTOLOGY_ID), Boolean.valueOf(this.isVirtualOntologyId));
		for (String localSemanticTypeId: this.semanticTypes){
			individual.addPropertyValue(obsOntology.owlModel.getOWLDatatypeProperty(NcboObsOntology.OWL_LOCAL_STT), localSemanticTypeId);
		} 
		individual.addPropertyValue(obsOntology.owlModel.getOWLDatatypeProperty(NcboObsOntology.OWL_LEVEL_MAX), Integer.valueOf(this.levelMax));
		for (String mappingType: this.mappingTypes){
			individual.addPropertyValue(obsOntology.owlModel.getOWLDatatypeProperty(NcboObsOntology.OWL_MAPPING_TYPE), mappingType);
		}		
		individual.addPropertyValue(obsOntology.owlModel.getOWLDatatypeProperty(NcboObsOntology.OWL_FILTERNUMBER), Boolean.valueOf(this.filterNumber));
		for (String stopWords: this.additionalStopWords){
			individual.addPropertyValue(obsOntology.owlModel.getOWLDatatypeProperty(NcboObsOntology.OWL_MORE_STOP_WORD), stopWords);
		}
		individual.addPropertyValue(obsOntology.owlModel.getOWLDatatypeProperty(NcboObsOntology.OWL_CASE_SENSITIVE_STOP_WORD), Boolean.valueOf(this.isStopWordsCaseSenstive));
		
		return individual;
	}
	
	/**
	 * Constructs a <code>String</code> with all attributes in name = value format.
	 * @return a <code>String</code> representation of this object.
	 */
	public String toString(){
	    final String TAB = ", ";
	    StringBuffer retValue = new StringBuffer();
	    retValue.append("[");
	    
	    if(this.email != null){
	    	 retValue.append("email = ").append(this.email).append(TAB);
		}		
        if(this.applicationid != null){
        	retValue.append("applicationid = ").append(this.applicationid).append(TAB);
		}        
        if(this.format != null){
        	retValue.append("format = ").append(this.format).append(TAB);
		}
	    
        retValue.append("longestOnly = ").append(this.longestOnly).append(TAB)
	        .append("wholeWordOnly = ").append(this.wholeWordOnly).append(TAB)
	        .append("stopWords = ").append(this.stopWords).append(TAB)
	        .append("minTermSize = ").append(this.minTermSize).append(TAB)
	        .append("scored = ").append(this.scored).append(TAB)
	        .append("withSynonyms = ").append(this.withSynonyms).append(TAB)
	        .append("ontologiesToKeepInResult = ").append(this.ontologiesToKeepInResult).append(TAB)
	        .append("isVirtualOntologyId = ").append(this.isVirtualOntologyId).append(TAB)
	        .append("semanticTypes = ").append(this.semanticTypes).append(TAB)	       
	        .append("levelMax = ").append(this.levelMax).append(TAB)
	        .append("mappingTypes = ").append(this.mappingTypes).append(TAB)
	        .append("additionalStopWords = ").append(this.additionalStopWords).append(TAB)
	        .append("withDefaultStopWords = ").append(this.withDefaultStopWords).append(TAB)
	        .append("isStopWordsCaseSenstive = ").append(this.isStopWordsCaseSenstive)
	        .append("]");
	    return retValue.toString();
	} 
}

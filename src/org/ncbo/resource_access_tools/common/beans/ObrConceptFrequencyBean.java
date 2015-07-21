package org.ncbo.resource_access_tools.common.beans;

public class ObrConceptFrequencyBean {
	
	private ConceptBean concept;
	private long counts;
	private float score;
	
	public ObrConceptFrequencyBean(ConceptBean concept, long counts, float score) {
		super();
		this.concept = concept;
		this.counts = counts;
		this.score = score;
	}

	public ConceptBean getConcept() {
		return concept;
	}

	public long getCounts() {
		return counts;
	}

	public float getScore() {
		return score;
	}  
	 
}

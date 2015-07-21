package org.ncbo.resource_access_tools.common.beans;

import org.ncbo.resource_access_tools.common.service.NcboObsOntology;

import edu.stanford.smi.protegex.owl.model.OWLIndividual;

/**
 * This class is a JavaBean representation of an ontology within OBS.
 *    
 * @author Clement Jonquet
 * @version 1.0
 * @created 24-Sep-2008 2:42:11 PM
 */
public class OntologyBean {

	private String localOntologyId;
	private String ontologyName;
	private String ontologyVersion;
	private String virtualOntologyId;
	
	public OntologyBean(String localOntologyId, String ontologyName, String ontologyVersion, String virtualOntologyId) {
		super();
		this.localOntologyId = localOntologyId;
		this.ontologyName = ontologyName;
		this.ontologyVersion = ontologyVersion;
		this.virtualOntologyId = virtualOntologyId;
	}

	/**
	 * Returns an ontology bean (OBS framework) with a bean from the ontologyAccess framework. 
	 */
	public OntologyBean(obs.ontologyAccess.bean.BioPortalFullOntologyBean ontologyBean){
		super();
		this.localOntologyId = ontologyBean.getId().toString();
		this.ontologyName = ontologyBean.getDisplayLabel();
		this.ontologyVersion = ontologyBean.getVersionNumber();
		this.virtualOntologyId = ontologyBean.getOntologyId().toString();
	}
	
	public String getLocalOntologyId() {
		return localOntologyId;
	}
	
	public String getOntologyName() {
		return ontologyName;
	}
	
	public String getOntologyVersion() {
		return ontologyVersion;
	}
	
	public String getVirtualOntologyId() {
		return virtualOntologyId;
	}
	
	protected void assignPropertyValue(OWLIndividual individual, NcboObsOntology obsOntology){
		individual.addPropertyValue(obsOntology.owlModel.getOWLDatatypeProperty(NcboObsOntology.OWL_LOCAL_ONTOLOGYID), this.localOntologyId);
		individual.addPropertyValue(obsOntology.owlModel.getOWLDatatypeProperty(NcboObsOntology.OWL_ONTOLOGY_NAME), this.ontologyName);
		individual.addPropertyValue(obsOntology.owlModel.getOWLDatatypeProperty(NcboObsOntology.OWL_ONTOLOGY_VERSION), this.ontologyVersion);
		individual.addPropertyValue(obsOntology.owlModel.getOWLDatatypeProperty(NcboObsOntology.OWL_VIRTUAL_ONTOLOGYID), this.virtualOntologyId);
	} 
		
	/**
	 * Constructs a <code>String</code> with all attributes in name = value format.
	 * @return a <code>String</code> representation of this object.
	 */
	public String toString(){
	    final String TAB = ", ";
	    StringBuffer retValue = new StringBuffer();
	    retValue.append("[")
	        .append(this.ontologyName).append(" (")
	        .append(this.localOntologyId).append(TAB)
	        .append(this.ontologyVersion).append(")")
	        .append(this.virtualOntologyId).append(")")
	        .append("]");
	    return retValue.toString();
	}
	
}
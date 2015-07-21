package org.ncbo.resource_access_tools.common.beans;

import java.util.ArrayList;

import org.ncbo.resource_access_tools.populate.Resource;

import obs.ontologyAccess.bean.BioPortalFullOntologyBean;

/**
 * This class is a JavaBean representation of an ontology with resources indexed by it within OBR.
 *  
 * @author Kuladip Yadav
 *
 */
public class ObrOntologyBean extends OntologyBean {
	
	private ArrayList<Resource> resources;

	/**
	 * @param localOntologyId
	 * @param ontologyName
	 * @param ontologyVersion
	 * @param virtualOntologyId
	 */
	public ObrOntologyBean(String localOntologyId, String ontologyName,
			String ontologyVersion, String virtualOntologyId) {
		super(localOntologyId, ontologyName, ontologyVersion, virtualOntologyId);
		// TODO Auto-generated constructor stub
	}

	/**
	 * @param ontologyBean
	 */
	public ObrOntologyBean(BioPortalFullOntologyBean ontologyBean) {
		super(ontologyBean);
		// TODO Auto-generated constructor stub
	}
	
	public ObrOntologyBean(OntologyBean ontologyBean, ArrayList<Resource> resources) {
		super(ontologyBean.getLocalOntologyId(), ontologyBean.getOntologyName(), ontologyBean.getOntologyVersion(), ontologyBean.getVirtualOntologyId());
		this.resources = resources;
	}

	/**
	 * @return the resources
	 */
	public ArrayList<Resource> getResources() {
		return resources;
	}

	/**
	 * @param resources the resources to set
	 */
	public void setResources(ArrayList<Resource> resources) {
		this.resources = resources;
	}
 
	
}

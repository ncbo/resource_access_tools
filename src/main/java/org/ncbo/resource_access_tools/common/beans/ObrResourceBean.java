package org.ncbo.resource_access_tools.common.beans;

import org.ncbo.resource_access_tools.populate.Resource;
import org.ncbo.resource_access_tools.populate.Structure;

import java.net.URL;
import java.util.ArrayList;

/**
 * This class is a JavaBean representation of an resources with ontologies used for indexed by it within OBR.
 *
 * @author Kuladip Yadav
 */
class ObrResourceBean extends Resource {

    private ArrayList<OntologyBean> ontologiesUsed;

    /**
     * @param resourceName
     * @param resourceId
     * @param resourceStructure
     * @param mainContext
     */
    public ObrResourceBean(String resourceName, String resourceId,
                           Structure resourceStructure, String mainContext) {
        super(resourceName, resourceId, resourceStructure, mainContext);
        // TODO Auto-generated constructor stub
    }

    /**
     * @param resourceName
     * @param resourceId
     * @param resourceStructure
     * @param mainContext
     * @param resourceURL
     * @param resourceElementURL
     * @param resourceDescription
     * @param resourceLogo
     */
    public ObrResourceBean(String resourceName, String resourceId,
                           Structure resourceStructure, String mainContext, URL resourceURL,
                           String resourceElementURL, String resourceDescription,
                           URL resourceLogo) {
        super(resourceName, resourceId, resourceStructure, mainContext,
                resourceURL, resourceElementURL, resourceDescription,
                resourceLogo);
        // TODO Auto-generated constructor stub
    }

    /**
     * @param resource
     * @param ontologiesUsed
     */
    public ObrResourceBean(Resource resource,
                           ArrayList<OntologyBean> ontologiesUsed) {
        super(resource.getResourceName(), resource.getResourceId(), resource.getResourceStructure(), resource.getMainContext(),
                resource.getResourceURL(), resource.getResourceElementURL(), resource.getResourceDescription(),
                resource.getResourceLogo());
        this.ontologiesUsed = ontologiesUsed;
    }

    /**
     * @return the ontologiesUsed
     */
    public ArrayList<OntologyBean> getOntologiesUsed() {
        return ontologiesUsed;
    }

    /**
     * @param ontologiesUsed the ontologiesUsed to set
     */
    public void setOntologiesUsed(ArrayList<OntologyBean> ontologiesUsed) {
        this.ontologiesUsed = ontologiesUsed;
    }


    public boolean hasOntologyBean(OntologyBean ontologyBean) {
        if (ontologiesUsed != null) {
            return ontologiesUsed.contains(ontologyBean);
        }

        return false;
    }


}

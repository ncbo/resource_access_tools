package org.ncbo.resource_access_tools.common.beans;


/**
 * Resource statistics beans wraps statistics for particular ontology.
 *
 * @author kyadav
 */
class ObrResourceStatisticsBean {

    private String localOntologyId;

    private ObrStatisticsBean statistics;


    public ObrResourceStatisticsBean() {

    }


    /**
     * @return the localOntologyId
     */
    public String getLocalOntologyId() {
        return localOntologyId;
    }


    /**
     * @param localOntologyId the localOntologyId to set
     */
    public void setLocalOntologyId(String localOntologyId) {
        this.localOntologyId = localOntologyId;
    }


    /**
     * @return the statistics
     */
    public ObrStatisticsBean getStatistics() {
        return statistics;
    }


    /**
     * @param statistics the statistics to set
     */
    public void setStatistics(ObrStatisticsBean statistics) {
        this.statistics = statistics;
    }


}

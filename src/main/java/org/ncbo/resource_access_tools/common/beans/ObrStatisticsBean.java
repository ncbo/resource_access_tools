package org.ncbo.resource_access_tools.common.beans;

/**
 * Statistics bean for resource index annotations which includes
 * indexed annotations , mgrep annotations, reported annotations
 *
 * @author kyadav
 */
public class ObrStatisticsBean {

    private long aggregatedAnnotations;
    private long mgrepAnnotations;
    private long reportedAnnotations;
    private long isaAnnotations;
    private long mappingAnnotations;


    public ObrStatisticsBean() {
        // TODO Auto-generated constructor stub
    }


    public ObrStatisticsBean(long aggregatedAnnotations, long mgrepAnnotations,
                             long reportedAnnotations, long isaAnnotations, long mappingAnnotations) {
        super();
        this.aggregatedAnnotations = aggregatedAnnotations;
        this.mgrepAnnotations = mgrepAnnotations;
        this.reportedAnnotations = reportedAnnotations;
        this.isaAnnotations = isaAnnotations;
        this.mappingAnnotations = mappingAnnotations;
    }


    /**
     * @return the aggregatedAnnotations
     */
    public long getAggregatedAnnotations() {
        return aggregatedAnnotations;
    }


    /**
     * @param aggregatedAnnotations the aggregatedAnnotations to set
     */
    public void setAggregatedAnnotations(long aggregatedAnnotations) {
        this.aggregatedAnnotations = aggregatedAnnotations;
    }


    /**
     * @return the mgrepAnnotations
     */
    public long getMgrepAnnotations() {
        return mgrepAnnotations;
    }


    /**
     * @param mgrepAnnotations the mgrepAnnotations to set
     */
    public void setMgrepAnnotations(long mgrepAnnotations) {
        this.mgrepAnnotations = mgrepAnnotations;
    }


    /**
     * @return the reportedAnnotations
     */
    public long getReportedAnnotations() {
        return reportedAnnotations;
    }


    /**
     * @param reportedAnnotations the reportedAnnotations to set
     */
    public void setReportedAnnotations(long reportedAnnotations) {
        this.reportedAnnotations = reportedAnnotations;
    }


    /**
     * @return the isaAnnotations
     */
    public long getIsaAnnotations() {
        return isaAnnotations;
    }


    /**
     * @param isaAnnotations the isaAnnotations to set
     */
    public void setIsaAnnotations(long isaAnnotations) {
        this.isaAnnotations = isaAnnotations;
    }


    /**
     * @return the mappingAnnotations
     */
    public long getMappingAnnotations() {
        return mappingAnnotations;
    }


    /**
     * @param mappingAnnotations the mappingAnnotations to set
     */
    public void setMappingAnnotations(long mappingAnnotations) {
        this.mappingAnnotations = mappingAnnotations;
    }


}

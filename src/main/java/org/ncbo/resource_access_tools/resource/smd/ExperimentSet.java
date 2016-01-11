package org.ncbo.resource_access_tools.resource.smd;

import java.util.HashSet;

/**
 * This class wraps description and set of experiment ids for SMD
 * experiment sets.
 *
 * @author kyadav
 */
class ExperimentSet {

    // Description of Experiment set
    private String description;

    // Set of experiment id's present in experiment set
    private HashSet<String> experimentIDs;

    /**
     * Constructor for this class.
     *
     * @param description   <code>String</code> of experiment set
     * @param experimentIDs <code>Set</code> of experiment ids
     */
    public ExperimentSet(String description, HashSet<String> experimentIDs) {
        this.description = description;
        this.experimentIDs = experimentIDs;
    }

    public String getDecription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public HashSet<String> getExperimentIDs() {
        return experimentIDs;
    }

    public void setExperimentIDs(HashSet<String> experimentIDs) {
        this.experimentIDs = experimentIDs;
    }
}

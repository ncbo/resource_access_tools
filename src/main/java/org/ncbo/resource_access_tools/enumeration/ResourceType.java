package org.ncbo.resource_access_tools.enumeration;

/**
 * Enum for resource type
 *
 * @author Kuladip Yadav
 */
public enum ResourceType {
    /**
     * For Big resources
     */
    BIG("Very Big Size Resource"),
    /**
     * For Medium size resource
     */
    MEDIUM("Mid Size Resource"),
    /**
     * For big resources
     */
    SMALL(" Small Resource");

    ResourceType(String description) {
        this.description = description;
    }

    private String description;

    /**
     * @return the description
     */
    public String getDescription() {
        return description;
    }

    /**
     * @param description the description to set
     */
    public void setDescription(String description) {
        this.description = description;
    }


}

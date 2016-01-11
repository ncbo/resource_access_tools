package org.ncbo.resource_access_tools.service.workflow;

import org.ncbo.resource_access_tools.dao.execution.ExecutionDao.ExecutionEntry;
import org.ncbo.resource_access_tools.resource.ResourceAccessTool;

/**
 * A service interface {@code ResourceIndexWorkflow} is main interface for workflow execution
 * it includes methods for populating slave obs tables and processing different resource tools with
 * population of elements and indexing them using slave obs tables.
 *
 * @author Kuladip Yadav
 */
public interface ResourceIndexWorkflow {

    /**
     * This method includes complete resource index workflow. It process resources and
     * update elements for them and annotated them using obs tables.
     * <p/>
     * <P>This methods process all the resources included in properties file.
     */
    void startResourceIndexWorkflow();

    /**
     * This method process individual resource and update elements for it.
     * Also it annotate that elements using obs tables and index them.
     *
     * @param resourceAccessTool a {@code ResourceAccessTool} to be processed.
     */
    void resourceProcessing(ResourceAccessTool resourceAccessTool, ExecutionEntry executionEntry);
}

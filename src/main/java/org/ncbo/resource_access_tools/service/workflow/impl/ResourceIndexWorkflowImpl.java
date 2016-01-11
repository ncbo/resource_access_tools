package org.ncbo.resource_access_tools.service.workflow.impl;

import org.apache.log4j.Logger;
import org.ncbo.resource_access_tools.dao.DaoFactory;
import org.ncbo.resource_access_tools.dao.execution.ExecutionDao.ExecutionEntry;
import org.ncbo.resource_access_tools.resource.ResourceAccessTool;
import org.ncbo.resource_access_tools.service.workflow.ResourceIndexWorkflow;
import org.ncbo.resource_access_tools.common.utils.ExecutionTimer;
import org.ncbo.resource_access_tools.util.MessageUtils;
import org.ncbo.resource_access_tools.util.StringUtilities;

import java.util.Date;

/**
 * A service  {@code ResourceIndexWorkflowImpl} is main service for workflow execution
 * it includes methods for populating slave obs tables and processing different resource tools with
 * population of elements and indexing them using slave obs tables.
 * <p/>
 * <p>Also, it includes functionality for removing duplicate ontologies.
 *
 * @author Kuladip Yadav
 */
public class ResourceIndexWorkflowImpl implements ResourceIndexWorkflow, DaoFactory {

    // Logger for this class
    private static final Logger logger = Logger.getLogger(ResourceIndexWorkflowImpl.class);

    /**
     * This method includes complete resource index workflow. It process resources and
     * update elements for them and annotated them using obs tables.
     * <p/>
     * <P>This methods process all the resources included in properties file.
     */
    public void startResourceIndexWorkflow() {
        ExecutionTimer workflowTimer = new ExecutionTimer();
        workflowTimer.start();
        // gets all resource ids for processing,
        String[] resourceIDs = StringUtilities.splitSecure(MessageUtils
                .getMessage("obr.resource.ids"));
        //Initialize the Execution timer
        ExecutionTimer timer = new ExecutionTimer();
        logger.info("***********************************************\n");
        logger.info("The Resources index Workflow Started.\n");
        for (String resourceID : resourceIDs) {
            ResourceAccessTool resourceAccessTool;
            ExecutionEntry executionEntry = new ExecutionEntry();
            executionEntry.setResourceId(resourceID);
            executionEntry.setExecutionBeginning(new Date());
            try {
                // Create resource tool object using reflection.
                resourceAccessTool = (ResourceAccessTool) Class.forName(
                        MessageUtils.getMessage("resource."
                                + resourceID.toLowerCase())).newInstance();
                logger.info("Start processing Resource " + resourceAccessTool.getToolResource().getResourceName() + "(" + resourceAccessTool.getToolResource().getResourceId() + ")....\n");
                timer.start();
                resourceProcessing(resourceAccessTool, executionEntry);
                timer.end();
                logger.info("Resource " + resourceAccessTool.getToolResource().getResourceName() + "(" + resourceAccessTool.getToolResource().getResourceId() + ") processed in: " + timer.millisecondsToTimeString(timer.duration()) + "\n");
            } catch (Exception e) {
                logger.error(
                        "Problem in creating resource tool for resource id : "
                                + resourceID, e);
            } finally {
                System.gc();
                executionEntry.setExecutionEnd(new Date());
                executionDao.addEntry(executionEntry);
            }

        }
        workflowTimer.end();
        logger.info("Resources index Workflow completed in : " + workflowTimer.millisecondsToTimeString(workflowTimer.duration()));
        logger.info("***********************************************\n");
    }

    /**
     * This method process individual resource and update elements for it.
     * Also it annotate that elements using obs tables and index them.
     *
     * @param resourceAccessTool a {@code ResourceAccessTool} to be processed.
     * @param executionEntry
     */
    public void resourceProcessing(ResourceAccessTool resourceAccessTool, ExecutionEntry executionEntry) {
        ExecutionTimer timer = new ExecutionTimer();
        ExecutionTimer timer1 = new ExecutionTimer();

        boolean reInitializeAllTables = Boolean.parseBoolean(MessageUtils.getMessage("obr.reinitialize.all"));
        boolean reInitializeAllTablesExceptElement = Boolean.parseBoolean(MessageUtils
                .getMessage("obr.reinitialize.only.annotation"));
        boolean updateResource = Boolean.parseBoolean(MessageUtils.getMessage("obr.update.resource"));
        // value for withCompleteDictionary parameter.
        boolean withCompleteDictionary = Boolean.parseBoolean(MessageUtils
                .getMessage("obr.dictionary.complete"));

        // Creating logger for resourceAcessTool
        Logger toolLogger = ResourceAccessTool.getLogger();
        timer1.start();
        toolLogger.info("**** Resource "
                + resourceAccessTool.getToolResource().getResourceId() + " processing");
        toolLogger.info("Workflow Parameters[withCompleteDictionary= " + withCompleteDictionary
                + ", updateResource= " + updateResource
                + ", reInitializeAllTables= " + reInitializeAllTables
                + ", reInitializeAllTablesExceptElement= " + reInitializeAllTablesExceptElement
                + " ]");
        // Adds resource entry into Resource Table(OBR_RT)
        resourceAccessTool.addResourceTableEntry();

        // Re-initialized tables
        if (reInitializeAllTables) {
            resourceAccessTool.reInitializeAllTables();
        } else if (reInitializeAllTablesExceptElement) {
            resourceAccessTool.reInitializeAllTablesExcept_ET();
        }

        logger.info("\n");
        if (resourceAccessTool.numberOfElement() == 0) {
            executionEntry.setFirstExecution(true);
        }

        // Update resource for new elements
        if (updateResource) {
            timer.start();
            toolLogger.info("*** Resource "
                    + resourceAccessTool.getToolResource().getResourceName() + " update processing");
            int nbElement = resourceAccessTool.updateResourceContent();
            resourceAccessTool.updateResourceUpdateInfo();

            timer.end();
            toolLogger.info("### Resource "
                    + resourceAccessTool.getToolResource().getResourceName()
                    + " updated with " + nbElement + " elements in : " + timer.millisecondsToTimeString(timer.duration()) + "\n");
        }

        timer1.end();
        toolLogger.info("#### Resource " + resourceAccessTool.getToolResource().getResourceName()
                + " processed in: "
                + timer1.millisecondsToTimeString(timer1.duration()));
    }

}

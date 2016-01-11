package org.ncbo.resource_access_tools;

import org.ncbo.resource_access_tools.service.workflow.ResourceIndexWorkflow;
import org.ncbo.resource_access_tools.service.workflow.impl.ResourceIndexWorkflowImpl;

class Main {

    public static void main(String[] args) {

        ResourceIndexWorkflow resourceIndexWorkflow = new ResourceIndexWorkflowImpl();

        try {
            // Populate resource index data
            resourceIndexWorkflow.startResourceIndexWorkflow();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }
}

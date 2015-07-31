package org.ncbo.resource_access_tools;

import org.ncbo.resource_access_tools.service.workflow.ResourceIndexWorkflow;
import org.ncbo.resource_access_tools.service.workflow.impl.ResourceIndexWorkflowImpl;
import org.ncbo.resource_access_tools.util.MessageUtils;

public class Main {

    public static void main(String[] args) {
		 
		ResourceIndexWorkflow resourceIndexWorkflow = new ResourceIndexWorkflowImpl();
		
		boolean processResources = Boolean.parseBoolean(MessageUtils.getMessage("obr.resources.process"));
		try{ 			
			// Populate resource index data
			if(processResources){				
				resourceIndexWorkflow.startResourceIndexWorkflow();
			}
		   			
		}catch (Exception e) {
			 e.printStackTrace();
		} 

	}
}

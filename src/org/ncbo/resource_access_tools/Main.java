package org.ncbo.resource_access_tools;

import org.ncbo.resource_access_tools.service.workflow.ResourceIndexWorkflow;
import org.ncbo.resource_access_tools.service.workflow.impl.ResourceIndexWorkflowImpl;
import org.ncbo.resource_access_tools.util.MessageUtils;

public class Main {

    public static void main(String[] args) {
		 
		ResourceIndexWorkflow resourceIndexWorkflow = new ResourceIndexWorkflowImpl();
		
		boolean poluateSlaveTables = Boolean.parseBoolean(MessageUtils.getMessage("obs.slave.populate"));
		boolean processResources = Boolean.parseBoolean(MessageUtils.getMessage("obr.resources.process"));
		boolean removeDuplicateOntologies = Boolean.parseBoolean(MessageUtils.getMessage("obs.slave.ontology.remove"));
		boolean excuteSyncronization = Boolean.parseBoolean(MessageUtils.getMessage("obr.database.sync"));
		boolean replicateObsTables = Boolean.parseBoolean(MessageUtils.getMessage("obr.database.sync.obs.tables"));
		try{ 
			// Populate obs tables from master database 
			if(poluateSlaveTables){	
				try{
					resourceIndexWorkflow.populateObsSlaveTables();					
				}catch (Exception e) {
					processResources=false;
					removeDuplicateOntologies=false;
					excuteSyncronization = false;
					e.printStackTrace();
				} 
			} 
			
			// Populate resource index data
			if(processResources){
				// Loading obs slave table
				//resourceIndexWorkflow.loadObsSlaveTablesIntoMemory();
				resourceIndexWorkflow.startResourceIndexWorkflow();
			}
		   
			// Remove duplicates.
			if(removeDuplicateOntologies){ 
				if(!processResources){
					// Loading obs slave table
					resourceIndexWorkflow.loadObsSlaveTablesIntoMemory();
				} 
				resourceIndexWorkflow.removeOntologyDuplicates();	 
			}
			
			// Execute replication mechanism
			if(excuteSyncronization){	
				// Replicate obs tables if one of replicateObsTables, poluateSlaveTables, removeDuplicateOntologies
				resourceIndexWorkflow.executeSyncronizationScript(replicateObsTables || poluateSlaveTables || removeDuplicateOntologies);
			}
			
		}catch (Exception e) {
			 e.printStackTrace();
		} 

	}
}

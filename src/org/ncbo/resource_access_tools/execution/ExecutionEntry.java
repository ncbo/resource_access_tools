package org.ncbo.resource_access_tools.execution;

import java.util.Date;

public class ExecutionEntry {
	
	private String resourceId;
	private int dictionaryId; 
	private boolean withCompleteDictionary;
	private int nbElement;
	private boolean firstExecution;
	private Date executionBeginning;
	private Date executionEnd;
	private long executionTime;
	
	public ExecutionEntry(){
		super();
	}
	
	public ExecutionEntry(String resourceId, int dictionaryId,
			boolean withCompleteDictionary, int nbElement,
			boolean firstExecution, Date executionBeginning,
			Date executionEnd, long executionTime) {
		super();
		this.resourceId = resourceId;
		this.dictionaryId = dictionaryId;
		this.withCompleteDictionary = withCompleteDictionary;
		this.nbElement = nbElement;
		this.firstExecution = firstExecution;
		this.executionBeginning = executionBeginning;
		this.executionEnd = executionEnd;
		this.executionTime = executionTime;
	} 
	
	public String getResourceId() {
		return resourceId;
	}

	public void setResourceId(String resourceId) {
		this.resourceId = resourceId;
	}

	public int getDictionaryId() {
		return dictionaryId;
	}

	public void setDictionaryId(int dictionaryId) {
		this.dictionaryId = dictionaryId;
	}

	public boolean isWithCompleteDictionary() {
		return withCompleteDictionary;
	}

	public void setWithCompleteDictionary(boolean withCompleteDictionary) {
		this.withCompleteDictionary = withCompleteDictionary;
	}

	public int getNbElement() {
		return nbElement;
	}

	public void setNbElement(int nbElement) {
		this.nbElement = nbElement;
	}

	public boolean isFirstExecution() {
		return firstExecution;
	}

	public void setFirstExecution(boolean firstExecution) {
		this.firstExecution = firstExecution;
	}

	public Date getExecutionBeginning() {
		return executionBeginning;
	}

	public void setExecutionBeginning(Date executionBeginning) {
		this.executionBeginning = executionBeginning;
	}

	public Date getExecutionEnd() {
		return executionEnd;
	}

	public void setExecutionEnd(Date executionEnd) {
		this.executionEnd = executionEnd;
	}

	public long getExecutionTime() {
		return executionTime;
	}

	public void setExecutionTime(long executionTime) {
		this.executionTime = executionTime;
	}

	public String toString(){
		StringBuffer sb = new StringBuffer();
		sb.append("ExecutionEntry: [ resourceId ");
		sb.append(this.resourceId);
		sb.append(", dictionaryId ");
		sb.append(this.dictionaryId);
		sb.append(", withCompleteDictionary ");
		sb.append(this.withCompleteDictionary);
		sb.append(", nbElement ");
		sb.append(this.nbElement);
		sb.append(", firstExecution ");
		sb.append(this.firstExecution);
		sb.append(", executionBeginning ");
		sb.append(this.executionBeginning);
		sb.append(", executionEnd ");
		sb.append(this.executionEnd);
		sb.append(", executionTime ");
		sb.append(this.executionTime);
		sb.append("]"); 
		return sb.toString();
	}
	
	 
}

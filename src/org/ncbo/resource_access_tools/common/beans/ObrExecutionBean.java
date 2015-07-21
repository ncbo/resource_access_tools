package org.ncbo.resource_access_tools.common.beans;

import java.util.Date;

/**
 * This class is a representation for a obr_execution table entry.
 * 
 * @author Kuladip Yadav
 * 
 */
public class ObrExecutionBean {

	private String resourceId;
	private int dictionaryId;
	private boolean withCompleteDictionary;
	private int elementCount;
	private boolean firstExecution;
	private Date executionBeginning;
	private Date executionEnd;
	private String executionTime;

	public ObrExecutionBean() {
		super();
	}

	public ObrExecutionBean(String resourceId, int dictionaryId,
			boolean withCompleteDictionary, int elementCount,
			boolean firstExecution, Date executionBeginning, Date executionEnd,
			String executionTime) {
		super();
		this.resourceId = resourceId;
		this.dictionaryId = dictionaryId;
		this.withCompleteDictionary = withCompleteDictionary;
		this.elementCount = elementCount;
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

	public int getElementCount() {
		return elementCount;
	}

	public void setElementCount(int elementCount) {
		this.elementCount = elementCount;
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

	public String getExecutionTime() {
		return executionTime;
	}

	public void setExecutionTime(String executionTime) {
		this.executionTime = executionTime;
	}

	public String toString() {
		StringBuffer sb = new StringBuffer();
		sb.append("execution: [ resourceId ");
		sb.append(this.resourceId);
		sb.append(", dictionaryId ");
		sb.append(this.dictionaryId);
		sb.append(", withCompleteDictionary ");
		sb.append(this.withCompleteDictionary);
		sb.append(", elementCount ");
		sb.append(this.elementCount);
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
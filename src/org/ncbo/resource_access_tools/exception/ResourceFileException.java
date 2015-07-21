package org.ncbo.resource_access_tools.exception;

public class ResourceFileException extends Exception {

	private static final long serialVersionUID = 1L;

	public ResourceFileException(String fileName) {
		super("Number of columns too short in file " + fileName);
	}

}

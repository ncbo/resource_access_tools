package org.ncbo.resource_access_tools.exception;

public class NoOntologyFoundException extends Exception {

	private static final long serialVersionUID = 1L;

	public NoOntologyFoundException() {
		super("No new ontology found");
	}

}

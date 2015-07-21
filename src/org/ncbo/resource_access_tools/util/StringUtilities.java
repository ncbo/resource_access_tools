package org.ncbo.resource_access_tools.util;

import org.ncbo.resource_access_tools.util.helper.StringHelper;

/**
 * 
 * @author Kuladip Yadav
 *
 */
public class StringUtilities implements StringHelper { 
	/**
	 * Removes special characters before inserting the text in the DB. 
	 */
	public static String escapeLine(String termName){
		return termName.replaceAll("\n|\r", " ");
	}
	
	/**
	 * 
	 * @param string
	 * @param regexp
	 * @return
	 */
	public static String[] splitSecure(String string, String regexp){
		String[] stringTab = string.split(regexp);
		for (int i = 0; i<stringTab.length; i++){
			if(stringTab[i]==null) stringTab[i]=  EMPTY_STRING;
		}
		return stringTab;
	}

}

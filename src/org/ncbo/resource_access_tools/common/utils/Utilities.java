package org.ncbo.resource_access_tools.common.utils;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.HashSet;
import java.util.Iterator;
import java.util.UUID;

import org.apache.log4j.Logger;

/**
* Contains some utilities (functions) used by OBS programs.
* 
* @author Clement Jonquet
* @version ontrez_v1
*/

public class Utilities {
	
	// Logger for Utilities 
	private static Logger logger = Logger.getLogger(Utilities.class);
	
	// Regular expression  for  non digit character
	public static final String NON_DIGIT_REGEX = "\\D";
	
	// Regular expression  for  non digit character
	public static final String SPACE_REGEX = "\\s";
	
	// String constant for empty string
	public static final String EMPTY_STRING = "";
	
	// String constant for empty string
	public static final String PLUS_STRING = "+";
	
	// String constant for empty string
	public static final String UTF8_STRING = "UTF-8";
	
	// String constant for slash string
	public static final String SLASH_STRING = "/";
	

	public static String getRandomString(int length) {
		UUID uuid = UUID.randomUUID();
		String myRandom = uuid.toString();
		return myRandom.substring(0,length);
		}
	
	public static void printStringHashSet(HashSet<String> hashSet){
		System.out.println(hashSet.toString());
	}
	/*
	public static void printStringHashSet(HashSet<String> hashSet){
		System.out.print("[");
		for (Iterator<String> it = hashSet.iterator(); it.hasNext();){
			System.out.print(it.next()+ ", ");
		}
		System.out.print("]");
		System.out.println("");
	}
	*/
	public static void printlnStringHashSet(HashSet<String> hashSet){
		System.out.print("[");
		for (Iterator<String> it = hashSet.iterator(); it.hasNext();){
			System.out.println(it.next()+ ", ");
		}
		System.out.print("]");
		System.out.println("");
	}
	
	public static String[] splitSecure(String string, String regexp){
		String[] stringTab = string.split(regexp);
		for (int i = 0; i<stringTab.length; i++){
			if(stringTab[i]==null) stringTab[i]= "";
		}
		return stringTab;
	}
	
	public static String[] splitSecure(String string, String regexp, int limit){
		String[] stringTab = string.split(regexp, limit);
		for (int i = 0; i<stringTab.length; i++){
			if(stringTab[i]==null) stringTab[i]= "";
		}
		return stringTab;
	}
	
	/**
	 * Convert an array of strings to one string.
	 * Put the 'separator' string between each element. 
	 */
	public static String arrayToString(String[] a, String separator) {
	    StringBuffer result = new StringBuffer();
	    if (a.length > 0) {
	        result.append(a[0]);
	        for (int i=1; i<a.length; i++) {
	            result.append(separator);
	            result.append(a[i]);
	        }
	    }
	    return result.toString();
	}
	
	/**
	 * Returns a HashSet<String> for a given String[]. If the array is null, then an empty set is returned.
	 */
	public static HashSet<String> arrayToHashSet(String[] array){
		HashSet<String> set = new HashSet<String>();
		if(array!=null){
			for (int i=0; i<array.length; i++){
				set.add(array[i]);
			}
		}
		return set;
	}
	
	/**	    
	 * This method encode String using UTF-8 character set.
	 * 
	 * @param stringToDecode
	 * @param encoding
	 * @return decoded string of given encoding character set.
	 */
	public static String decode(String stringToDecode, String encoding){
		try {
			return URLDecoder.decode(stringToDecode, encoding);
		} catch (UnsupportedEncodingException e) {
			logger.error("Problem in decoding string "+ stringToDecode +" with encoding "+ encoding);
		}		
		return null;
	} 
	
}

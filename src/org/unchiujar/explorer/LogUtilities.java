package org.unchiujar.explorer;

public class LogUtilities {

    /**
     * Logging utility method used for formatting a list of numbers in the
     * [x,y,z] format.
     * 
     * @param numbers
     *            the numbers to be formated
     * @return a formatted string
     */
    public static String numberLogList(double... numbers) {
	String formatted = "[";
	for (double d : numbers) {
	    formatted += d + ",";
	}
	return formatted + "\b]";
    }
}

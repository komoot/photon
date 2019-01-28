// This software is released into the Public Domain.
// this is nicked from osmosis by Brett Henderson https://github.com/openstreetmap/osmosis

package de.komoot.photon.utils;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.util.Locale;


/**
 * Formats replication sequence numbers into file names.
 */
public class SequenceFormatter {
	
	private NumberFormat sequenceFormat;
	

	/**
	 * Creates a new instance.
	 * 
	 * @param minimumLength
	 *            The minimum length file sequence string to generate. For example, setting a length
	 *            of 2 will generate sequence numbers from "00" to "99".
	 * @param groupingLength
	 *            The number of characters to write before separating with a '/' character. Used for
	 *            creating sequence numbers to be written to files in a nested directory structure.
	 */
	public SequenceFormatter(int minimumLength, int groupingLength) {
		DecimalFormatSymbols formatSymbols;
		StringBuilder formatString;
		
		formatSymbols = new DecimalFormatSymbols(Locale.US);
		formatSymbols.setGroupingSeparator('/');
		
		formatString = new StringBuilder();
		for (int i = 0; i < minimumLength || i <= groupingLength; i++) {
			if (i > 0 && groupingLength > 0 && i % groupingLength == 0) {
				formatString.append(',');
			}
			
			if (i < minimumLength) {
				formatString.append('0');
			} else {
				formatString.append('#');
			}
		}
		formatString.reverse();
		
		this.sequenceFormat = new DecimalFormat(formatString.toString(), formatSymbols);
	}


	/**
	 * Formats the sequence number into a file name. Any sub-directories required will be
	 * automatically created.
	 * 
	 * @param sequenceNumber
	 *            The sequence number.
	 * @param fileNameSuffix
	 *            The suffix to append to the end of the file name.
	 * @return The formatted file name.
	 */
	public String getFormattedName(long sequenceNumber, String fileNameSuffix) {
		String fileName;
		
		fileName = sequenceFormat.format(sequenceNumber) + fileNameSuffix;
		
		return fileName;
	}
}

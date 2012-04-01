package com.nolanlawson.apptracker.util;

public class FlagUtil {

	/**
	 * Returns true if "flags" contains "flag"
	 * @param flags
	 * @param flag
	 */
	public static boolean hasFlag(int flags, int flag) {
		return (flags & flag) == flag;
	}
	
}

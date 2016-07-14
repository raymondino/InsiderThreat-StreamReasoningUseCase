package com.tw.rpi.edu.si.utilities;

import java.util.Comparator;

public class ComparableStatementComparator implements Comparator<ComparableStatement>{

    @Override
	public int compare(ComparableStatement a, ComparableStatement b) {
    	if(a == null && b == null)
    		return 0;
    	else if (a != null && b == null)
    		return 1;
    	else if (a == null && b != null)
    		return -1;
    	else    	
    		return a.compareTo(b);
	}
}

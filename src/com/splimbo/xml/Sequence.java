package com.splimbo.xml;

import java.util.ArrayList;

public class Sequence extends Option {
	public static Long id = new Long(0);
	public Long currentId = id;
	public int currentIndex = -1;
	public ArrayList<Option> options = new ArrayList<Option>();
	public String condition = "true";

	public Sequence(){
		id++;
	}
	public String toString(){
		return "[Sequence => "+super.toString()
				+ "; currentId: "+ currentId
				+ "; currentIndex: "+ currentIndex
				+ "; options: "+ options.size()
				+ "]"; 	
	}
}

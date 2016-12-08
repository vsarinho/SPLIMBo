package com.splimbo.xml;

import java.util.ArrayList;
import java.util.Iterator;


public class Command extends Option {
	public String condition = "true";
	
	public ArrayList<HyperText> hyperTexts = new ArrayList<HyperText>();
	public ArrayList<Exec> execs = new ArrayList<Exec>();
	
	public String toString(){
		StringBuffer hyperTextsBuffer = new StringBuffer();
		
		Iterator<HyperText> iter = hyperTexts.iterator();
		while (iter.hasNext()){
			hyperTextsBuffer.append(iter.next().toString());
		}
				
		return "[Command => "+super.toString() 
				+ "; hyperTexts: "+ hyperTexts.size()
				+ "; execs: "+ execs.size()
				+"]"; 
				
	}
}

package com.splimbo.xml;

public abstract class Option {
	public String description = "";
	public String keycode = "";
	public Option parent = null;
	
	public String getKeyText(){
		return keycode + " - " + description;
	}
	
	public String toString(){
		return "[Option => keycode: "+ keycode + "; description: "+ description + "; parent: "+parent+"]"; 
				
	}
}

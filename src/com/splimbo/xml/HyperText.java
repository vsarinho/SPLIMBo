package com.splimbo.xml;


public class HyperText extends Option {
	public String content = "";
	public String type = "text"; // file, url
	public boolean destroyAfterShow = false;
	public String receiver = ""; 
	public String receiverUrl = "s.whatsapp.net";
	
	public String toString(){
		return "[HyperText => "+super.toString() + "; content: "+ content + "; type: "+ type + "]"; 				
	}
}

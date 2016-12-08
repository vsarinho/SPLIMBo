package com.splimbo.xml;


public class Prompt extends Command {
	public static String DEFAULT_URL = "s.whatsapp.net";
	
	public String receiver = ""; 
	public String receiverUrl = DEFAULT_URL;
	public String messageToSend = "";
	public String confirmationMessage = "";
	public String validation = "";
	public Exec execAfterConfirmation = null;
	
	public String toString(){
		return "[Prompt => "+super.toString() + "; receiver: "+ receiver + "; receiverUrl: "
							+ receiverUrl + "; message: "+ messageToSend  + "; validation: "+ validation+
							"; execAfterConfirmation: "+execAfterConfirmation+"]"; 
				
	}
}

package com.splimbo.xml;

import java.io.*;
import javax.xml.parsers.*;
import org.w3c.dom.*;

import com.splimbo.XMLZap;

public class XMLParser {	
	
	static boolean debug = false;
	
	public static void main(String[] args) throws Exception {
		XMLZap teste = new XMLZap();
		Option menu = loadXMLConfig("<my_zapapp_config.xml>", teste);
		
		System.out.println("path:"+teste.currentPath + "\n" + menu);
	}
	
	public static Option loadXMLConfig(String xmlFile, XMLZap xmlZap) {
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		DocumentBuilder builder;
		Document document = null;
		try {
			builder = factory.newDocumentBuilder();
			document = builder.parse(new File(xmlFile));	
			
			NodeList root = document.getChildNodes();			 
			Node zapapp = getNode("zapapp", root);
			xmlZap.currentPath = getNodeAttr("path", zapapp);
			
			return makeTree(zapapp);
		} 
		catch (Exception e) {
			e.printStackTrace();
		}
		
		return null;
	}
	
	public static Option makeTree(Node root){
		if (debug) System.out.println("makeTree "+root.getNodeName());
		
		NodeList nodes = root.getChildNodes();
    	for (int x = 0; x < nodes.getLength(); x++ ) {
	        Node node = nodes.item(x);
	        if (node.getNodeName().equalsIgnoreCase("command")) {
	        	return makeCommand(null, node);
	        }
	        else if (node.getNodeName().equalsIgnoreCase("prompt")) {
	        	return makePrompt(null, node);
	        }	
	        else if (node.getNodeName().equalsIgnoreCase("menu")) {	        	
	        	return makeMenu(null, node);
		    }
	        else if (node.getNodeName().equalsIgnoreCase("sequence")) {	        	
	        	return makeSequence(null, node);
		    }
	    }
    	
    	return null;
	}
	
	public static Menu makeMenu(Option menuParent, Node root){
		if (debug) System.out.println("makeMenu "+root.getNodeName());
		Menu result = new Menu();
		result.parent = menuParent;
		
		boolean isHeader = true;
		
		NodeList nodes = root.getChildNodes();
	    for (int x = 0; x < nodes.getLength(); x++ ) {
	        Node node = nodes.item(x);
	        
	        if (node.getNodeName().equalsIgnoreCase("command")) {
	        	result.options.add(makeCommand(result, node));
	        	isHeader = false;
	        }
	        else if (node.getNodeName().equalsIgnoreCase("prompt")) {
	        	result.options.add(makePrompt(result, node));
	        	isHeader = false;
	        }	
	        else if (node.getNodeName().equalsIgnoreCase("menu")) {
	        	result.options.add(makeMenu(result, node));
	        	isHeader = false;
	        }
	        else if (node.getNodeName().equalsIgnoreCase("sequence")) {
	        	result.options.add(makeSequence(result, node));
	        	isHeader = false;
	        }
	        else if (node.getNodeName().equalsIgnoreCase("image") || 
	        		node.getNodeName().equalsIgnoreCase("text") ||
	        		node.getNodeName().equalsIgnoreCase("http-text") || 
	        		node.getNodeName().equalsIgnoreCase("http-image") || 
	        		node.getNodeName().equalsIgnoreCase("video")) {	        	
	        	if (isHeader)
	        		result.headers.add(makeHyperText(node));
	        	else
	        		result.footers.add(makeHyperText(node));
		    }	    		        
	    }
	    
		String includeBackOption = getNodeAttr("includeBackOption", root);
		if (includeBackOption == null || "".equals(includeBackOption))
			includeBackOption = "true";    		        	
		result.includeBackOption = Boolean.parseBoolean(includeBackOption);
		
		String execIfOneInstruction = getNodeAttr("execIfOneInstruction", root);
		if (execIfOneInstruction == null || "".equals(execIfOneInstruction))
			execIfOneInstruction = "true";    		
		result.execIfOneInstruction = Boolean.parseBoolean(execIfOneInstruction);	        	

	    result.keycode = getNodeAttr("keycode", root);
    	result.description = getNodeAttr("description", root);
    	result.condition = getNodeAttr("condition", root);
    	
	    return result;
	}
	
	public static Option makeCommand(Option currentParent, Node root){
		if (debug) System.out.println("makeCommand "+root.getNodeName());
		Node sequenceNode = null;
		Node menuNode = null;
		NodeList nodes = root.getChildNodes();
	    for (int x = 0; x < nodes.getLength(); x++ ) {
	        Node node = nodes.item(x);
	         
	        if (node.getNodeName().equalsIgnoreCase("menu")) {	 
	        	menuNode = node;
	        	if (debug) System.out.println("isMenu!!");
	        }
	        else if (node.getNodeName().equalsIgnoreCase("sequence")) {	 
	        	sequenceNode = node;
	        	if (debug) System.out.println("isSequence!!");
	        }
	    }
    	Option result = null;
    	
	    if (sequenceNode != null)
	    	result = makeSequence(currentParent, sequenceNode);
	    else if (menuNode != null)
	    	result = makeMenu(currentParent, menuNode);
	    else {
	    	result = new Command();
	    	result.parent = currentParent;
	    	
		    for (int x = 0; x < nodes.getLength(); x++ ) {
		        Node node = nodes.item(x);
		        
		        if (node.getNodeName().equalsIgnoreCase("image") || 
		        	node.getNodeName().equalsIgnoreCase("video") || 
	        		node.getNodeName().equalsIgnoreCase("http-text") || 
	        		node.getNodeName().equalsIgnoreCase("http-image") || 
		        	node.getNodeName().equalsIgnoreCase("text")) {	
		        	((Command) result).hyperTexts.add(makeHyperText(node));
		        }
		        else if (node.getNodeName().equalsIgnoreCase("exec")) {	
			        ((Command) result).execs.add(makeExec(node));
			    }
		    }
		    
		    ((Command) result).condition = getNodeAttr("condition", root);
	    }
	    
	    result.keycode = getNodeAttr("keycode", root);
    	result.description = getNodeAttr("description", root);
    	
    	return result;
	}
	
	public static Option makeSequence(Option currentParent, Node root){
		if (debug) System.out.println("makeSequence "+root.getNodeName());
		Sequence result = null;
    	
    	result = new Sequence();
    	result.parent = currentParent;
    	
    	NodeList nodes = root.getChildNodes();
    	for (int x = 0; x < nodes.getLength(); x++ ) {
	        Node node = nodes.item(x);
	        if (node.getNodeName().equalsIgnoreCase("command")) {
	        	result.options.add(makeCommand(result, node));
	        }
	        else if (node.getNodeName().equalsIgnoreCase("prompt")) {
	        	result.options.add(makePrompt(result, node));
	        }	
	        else if (node.getNodeName().equalsIgnoreCase("menu")) {	        	
	        	result.options.add(makeMenu(result, node));
		    }
	        else if (node.getNodeName().equalsIgnoreCase("sequence")) {	        	
	        	result.options.add(makeSequence(result, node));
		    }
	    }			    
	    
	    result.keycode = getNodeAttr("keycode", root);
    	result.description = getNodeAttr("description", root);
    	result.condition = getNodeAttr("condition", root);
    	
    	return result;
	}
		
	public static HyperText makeHyperText(Node root){
		if (debug) System.out.println("makeHyperText "+root.getNodeName());
		HyperText result = new HyperText();
		
		result.type = root.getNodeName().toLowerCase();	
		result.content = getNodeValue(root);
		String destroyAfterShow = getNodeAttr("destroyAfterShow", root);
		if (destroyAfterShow!= null && "true".equals(destroyAfterShow.toLowerCase())) 
			result.destroyAfterShow = true;
		
		result.receiver = getNodeAttr("receiver", root);
		if (result.receiver == null) result.receiver = ""; 
		
    	result.receiverUrl = getNodeAttr("receiverUrl", root);
    	if (result.receiverUrl == null) result.receiverUrl = ""; 
    	
		return result;
	}
	
	public static Exec makeExec(Node root){
		if (debug) System.out.println("makeExec "+root.getNodeName());
		Exec result = new Exec();
		
		result.instruction = getNodeValue(root);
		
		return result;
	}
	
	public static Prompt makePrompt(Option menuParent, Node root){
		if (debug) System.out.println("makePrompt "+root.getNodeName());
		Prompt result = new Prompt();
    	result.parent = menuParent;
    	
    	NodeList nodes = root.getChildNodes();
	    for (int x = 0; x < nodes.getLength(); x++ ) {
	        Node node = nodes.item(x);
	        
	        if (node.getNodeName().equalsIgnoreCase("image") || 
	        	node.getNodeName().equalsIgnoreCase("video") || 
	        	node.getNodeName().equalsIgnoreCase("http-text") || 
	        	node.getNodeName().equalsIgnoreCase("http-image") || 
	        	node.getNodeName().equalsIgnoreCase("text")) {	
	        	((Command) result).hyperTexts.add((HyperText) makeHyperText(node));
	        }
	    }	
	    
	    result.keycode = getNodeAttr("keycode", root);
    	result.description = getNodeAttr("description", root);
    	result.condition = getNodeAttr("condition", root);
    	
    	result.receiver = getNodeAttr("receiver", root);
    	result.receiverUrl = getNodeAttr("receiverUrl", root);
    	if (result.receiverUrl == null || "".equals(result.receiverUrl))
    		result.receiverUrl = Prompt.DEFAULT_URL;    	
    	result.messageToSend = getNodeAttr("messageToSend", root);
    	result.confirmationMessage = getNodeAttr("confirmationMessage", root);
    	result.validation = getNodeAttr("validation", root);   
		
    	String execAfterConfirmationAttr = getNodeAttr("execAfterConfirmation", root);
    	if (execAfterConfirmationAttr != null && !"".equals(execAfterConfirmationAttr)){
    		Exec execAfterConfirmation = new Exec();
    		execAfterConfirmation.instruction = execAfterConfirmationAttr;
    		result.execAfterConfirmation = execAfterConfirmation;    	
    	}
    	
		return result;
	}
	
	protected static Node getNode(String tagName, NodeList nodes) {
	    for ( int x = 0; x < nodes.getLength(); x++ ) {
	        Node node = nodes.item(x);
	        if (node.getNodeName().equalsIgnoreCase(tagName)) {
	            return node;
	        }
	    }
	 
	    return null;
	}
	 
	protected static String getNodeValue( Node node ) {
	    NodeList childNodes = node.getChildNodes();
	    for (int x = 0; x < childNodes.getLength(); x++ ) {
	        Node data = childNodes.item(x);
	        if ( data.getNodeType() == Node.TEXT_NODE )
	            return data.getNodeValue();
	    }
	    return "";
	}
	 
	protected static String getNodeValue(String tagName, NodeList nodes ) {
	    for ( int x = 0; x < nodes.getLength(); x++ ) {
	        Node node = nodes.item(x);
	        if (node.getNodeName().equalsIgnoreCase(tagName)) {
	            NodeList childNodes = node.getChildNodes();
	            for (int y = 0; y < childNodes.getLength(); y++ ) {
	                Node data = childNodes.item(y);
	                if ( data.getNodeType() == Node.TEXT_NODE )
	                    return data.getNodeValue();
	            }
	        }
	    }
	    return "";
	}
	 
	protected static String getNodeAttr(String attrName, Node node ) {
	    NamedNodeMap attrs = node.getAttributes();
	    for (int y = 0; y < attrs.getLength(); y++ ) {
	        Node attr = attrs.item(y);
	        if (attr.getNodeName().equalsIgnoreCase(attrName)) {
	            return attr.getNodeValue();
	        }
	    }
	    return "";
	}
	 
	protected static String getNodeAttr(String tagName, String attrName, NodeList nodes ) {
	    for ( int x = 0; x < nodes.getLength(); x++ ) {
	        Node node = nodes.item(x);
	        if (node.getNodeName().equalsIgnoreCase(tagName)) {
	            NodeList childNodes = node.getChildNodes();
	            for (int y = 0; y < childNodes.getLength(); y++ ) {
	                Node data = childNodes.item(y);
	                if ( data.getNodeType() == Node.ATTRIBUTE_NODE ) {
	                    if ( data.getNodeName().equalsIgnoreCase(attrName) )
	                        return data.getNodeValue();
	                }
	            }
	        }
	    }
	 
	    return "";
	}

}

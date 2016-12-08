package com.splimbo.xml;

import java.util.*;

import org.jbpm.gop.Execution;

import com.splimbo.Messenger;
import com.splimbo.Util;
import com.splimbo.XMLZap;

public class Menu extends Option {
	public boolean includeBackOption = true;
	public boolean execIfOneInstruction = true;
	public String condition = "true";
	
	public HashMap<Integer,Option> instructions = new HashMap<Integer,Option>();
	
	public ArrayList<HyperText> headers = new ArrayList<HyperText>();
	public ArrayList<Option> options = new ArrayList<Option>();
	public ArrayList<HyperText> footers = new ArrayList<HyperText>();
	
	public String toString(){
		return "[Menu => "+super.toString() 
				+ "; includeBackOption: "+ includeBackOption 
				+ "; headers: "+ headers.size()
				+ "; options: "+ options.size()
				+ "; footers: "+ footers.size()
				+ "]"; 	
	}
	
	public String getMenu(Execution execution){
		String result = "";
		
		// header
		ArrayList<HyperText> headersToRemove = new ArrayList<HyperText>(); 
		
		Iterator<HyperText> iterHeader = headers.iterator();
		while (iterHeader.hasNext()){
			HyperText hyperText = iterHeader.next();
			
			String hyperTextContent = XMLZap.applyContext(execution, hyperText.content);
			
			if ("text".equals(hyperText.type))
				result += hyperTextContent;
			else {
				System.out.println("menu-hyperTextContent: "+hyperTextContent);
				
				Messenger.sendMedia((String) execution.variables.get("jidServer"), 
									(String) execution.variables.get("jidClient"),
									(String) execution.variables.get("jidUrlClient"), 
									((String) execution.variables.get("currentPath")) + hyperTextContent, 
									hyperTextContent.substring(hyperTextContent.lastIndexOf(".")+1, hyperTextContent.length()));
			}
			if (hyperText.destroyAfterShow)	headersToRemove.add(hyperText);
			
		}	
		// remove headers to destroy
		Iterator<HyperText> iterHeadersToRemove = headersToRemove.iterator();
		while (iterHeadersToRemove.hasNext()){
				headers.remove(iterHeadersToRemove.next());
		}
		
		// options
		int order = 1;
		Iterator<Option> iterOptions = options.iterator();
		while (iterOptions.hasNext()){
			Option currentOption = iterOptions.next();
			
			boolean canAddOption = true;
			
			if (currentOption instanceof Command)
				canAddOption = XMLZap.evaluateCondition(execution, ((Command) currentOption).condition);					
			
			if (currentOption instanceof Sequence)
				canAddOption = XMLZap.evaluateCondition(execution, ((Sequence) currentOption).condition);					
			
			
			if (canAddOption){
				String keyText = currentOption.getKeyText();
				if (keyText.contains("System.order")){
					keyText = XMLZap.applyContext(execution, keyText.replace("System.order",""+order));	
					instructions.put(order, currentOption);
				}
				
				result += "\n" + keyText;
				order++;
			}
		}
		
		// footer
		ArrayList<HyperText> footersToRemove = new ArrayList<HyperText>(); 
		
		Iterator<HyperText> iterFooter = footers.iterator();
		while (iterFooter.hasNext()){
			HyperText hyperText = iterFooter.next();
			
			String hyperTextContent = XMLZap.applyContext(execution, hyperText.content);
			
			if ("text".equals(hyperText.type))
				result += hyperTextContent;
			else {
				System.out.println("hyperTextContent: "+hyperTextContent);
				Messenger.sendMedia((String) execution.variables.get("jidServer"), 
									(String) execution.variables.get("jidClient"),
									(String) execution.variables.get("jidUrlClient"),
									((String) execution.variables.get("currentPath")) +  hyperTextContent, 
									hyperTextContent.substring(hyperTextContent.lastIndexOf(".")+1, hyperTextContent.length()));
			}		
			if (hyperText.destroyAfterShow)	footersToRemove.add(hyperText);
		}	
		
		// remove footers to destroy
		Iterator<HyperText> iterFootersToRemove = footersToRemove.iterator();
		while (iterFootersToRemove.hasNext()){
				footers.remove(iterFootersToRemove.next());
		}
		
		if (includeBackOption)
			result += "\n\n"+Util.RETURN_TEXT;
		
		return result;
	}
}

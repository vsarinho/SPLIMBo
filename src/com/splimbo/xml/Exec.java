package com.splimbo.xml;

import org.jbpm.gop.Execution;

import com.splimbo.XMLZap;

public class Exec extends Option {
	public String instruction = "";
	
	public String toString(){
		return "[Exec => "+super.toString() + "; instruction: "+ instruction + "]"; 				
	}
	
	public void perform(Execution execution){
		String[] routines = instruction.split(";;;");
		
		for (int i = 0; i<routines.length; i++){
			routines[i] = routines[i].trim();
			System.out.println("routine: "+routines[i]);
			
			if ("exit".equals(routines[i].toLowerCase())){
				Option currentOption = (Option) execution.variables.get("currentOption");
				
				while (!(currentOption instanceof Sequence) && currentOption != null && currentOption.parent != null)
					currentOption = currentOption.parent;
				
				System.out.println("forward => currentOption:"+currentOption);
				
				execution.variables.put("currentOption", currentOption);
			}
			else if ("++".equals(routines[i].substring(0,2))){
				String variable = (String) execution.variables.get(routines[i].substring(2,routines[i].length()));
				int incrementedValue = 0;
				try {
					incrementedValue = Integer.parseInt(variable) + 1;
				}
				catch (Exception e){
					e.printStackTrace();
				}
				execution.variables.put(routines[i].substring(2,routines[i].length()),""+incrementedValue);
			}
			else if (routines[i].substring(0,5).toUpperCase().contains("SQL.")){
				XMLZap.evaluateSQL(execution, routines[i]);
			}			
			else {
				String variable = routines[i].substring(0,routines[i].indexOf("="));
				String expression = routines[i].substring(routines[i].indexOf("=")+1, routines[i].length());
				
				if (expression.toUpperCase().contains("SQL."))
					expression = XMLZap.evaluateSQL(execution, expression);
				else
					expression = XMLZap.applyContext(execution, expression);					
				
				System.out.println("atribution: "+variable + " = " +expression);
				execution.variables.put(variable, expression);											
			}
		}
	}
}

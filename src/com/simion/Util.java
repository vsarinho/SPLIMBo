package com.simion;

public class Util {
	public static String RETURN_TEXT = "V - Voltar";
	public static String RETURN_OPTION = "V";
	public static String DEFAULT_PATH = "/home/victor";
	public static String APPS_PATH = DEFAULT_PATH+"/projetos/ZapServer/";
	public static String ZAPSERVER_DATABASE = "zapserver";
	
	public static boolean isNumeric(String str)
	{
		if ("".equals(str) || str == null) return false;
		
	    for (char c : str.toCharArray())
	    {
	        if (!Character.isDigit(c)) return false;
	    }
	    return true;
	}
}

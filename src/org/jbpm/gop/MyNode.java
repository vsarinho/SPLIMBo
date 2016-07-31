package org.jbpm.gop;

public class MyNode extends Node
{
	
	public MyNode(String name)
	{
		super(name);
	}
	public MyNode(String name, Action initialAction)
	{
		this(name);
		addAction("enter-node", initialAction);
	}
}

package org.jbpm.gop;

import java.util.*;

/** a node in the process graph */
public class Node {
  
  public String name;
  /** maps events to transitions */
  Map<String,Transition> transitions = new HashMap<String,Transition>();
  /** maps events to actions */
  Map<String,List<Action>> actions = new HashMap<String,List<Action>>();
  
  public Node(String name) {
    this.name = name;
  }
  
  /** create a new transition to the destination node and 
   * associate it with the given event */ 
  public void addTransition(String event, Node destination) {
    transitions.put(event, new Transition(destination));
  }

  /** add the action to the given event */
  public void addAction(String event, Action action) {
    if (actions.containsKey(event)) {
      actions.get(event).add(action);
    } else {
      List<Action> eventActions = new ArrayList<Action>();
      eventActions.add(action);
      actions.put(event, eventActions);
    }
  }

  /** to be overriden by Node implementations. The default doesn't 
   * propagate the execution so it behaves as a wait state. */
  public void execute(Execution execution) {
    System.out.println("arrived in wait state "+this);
  }

  public String toString() { return "node '"+name+"'"; }
}

package org.jbpm.gop;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** one path of execution */
public class Execution {

  /** pointer to the current node */ 
  public Node node = null;
  public Map<String,Object> variables = new HashMap<String,Object>();
  
  /** an execution always starts in a given node */
  public Execution(Node node) {
    this.node = node;
  }

  /** executes the current node's actions and takes the event's transition */
  public void event(String event) {
    System.out.println(this+" received event '"+event+"' on "+node);
    fire(event);
    if (node.transitions.containsKey(event)) {
      //System.out.println(this+" leaves "+node);
      fire("leave-node");
      take(node.transitions.get(event));
    }
  }
  
  /** take a transition */
  void take(Transition transition) {
    //System.out.println(this+" takes transition to "+transition.destination);
    node = transition.destination;
    enter(transition.destination);
  }
  
  /** enter the next node */
  public void enter(Node node) {
    //System.out.println(this+" enters "+node);
    fire("enter-node");
    node.execute(this);
  }

  /** fires the actions of a node for a specific event */
  void fire(String event) {
    List<Action> eventActions = node.actions.get(event);
    if (eventActions!=null) {
      //System.out.println(this+" fires actions for event '"+event);
      for (Action action : eventActions) 
        action.execute(this);
    }
  }
  
  public String toString() {return "execution";}
}

package org.jbpm.gop;

/** a command that can be injected into a process execution */
public interface Action {

  /** to be overriden by Action implementations */
  void execute(Execution execution);
}

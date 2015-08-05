package com.jetbrains.jetpad.vclang.term.definition;

import com.jetbrains.jetpad.vclang.term.expr.Expression;
import com.jetbrains.jetpad.vclang.term.expr.arg.Argument;
import com.jetbrains.jetpad.vclang.term.expr.arg.Utils;

import java.util.List;

public class OverriddenDefinition extends FunctionDefinition {
  private List<FunctionDefinition> myOverriddenFunctions;

  public OverriddenDefinition(Utils.Name name, Definition parent, Precedence precedence, Arrow arrow) {
    super(name, parent, precedence, arrow);
    myOverriddenFunctions = null;
  }

  public OverriddenDefinition(Utils.Name name, Definition parent, Precedence precedence, List<Argument> arguments, Expression resultType, Arrow arrow, Expression term, List<FunctionDefinition> overriddenFunctions) {
    super(name, parent, precedence, arguments, resultType, arrow, term);
    myOverriddenFunctions = overriddenFunctions;
  }

  @Override
  public List<FunctionDefinition> getOverriddenFunctions() {
    return myOverriddenFunctions;
  }

  public void setOverriddenFunctions(List<FunctionDefinition> overriddenFunctions) {
    myOverriddenFunctions = overriddenFunctions;
  }
}

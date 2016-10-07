package com.jetbrains.jetpad.vclang.term.definition;

import com.jetbrains.jetpad.vclang.naming.namespace.EmptyNamespace;
import com.jetbrains.jetpad.vclang.naming.namespace.Namespace;
import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.context.binding.Binding;
import com.jetbrains.jetpad.vclang.term.context.param.DependentLink;
import com.jetbrains.jetpad.vclang.term.context.param.EmptyDependentLink;
import com.jetbrains.jetpad.vclang.term.expr.DefCallExpression;
import com.jetbrains.jetpad.vclang.term.expr.Expression;
import com.jetbrains.jetpad.vclang.term.expr.subst.LevelSubstitution;
import com.jetbrains.jetpad.vclang.term.expr.type.Type;

import java.util.ArrayList;
import java.util.List;

public abstract class Definition implements Referable {
  protected List<Binding> myPolyParams = new ArrayList<>();
  private boolean myHasErrors;
  private ClassDefinition myThisClass;
  private Abstract.Definition myAbstractDefinition;

  public Definition(Abstract.Definition abstractDef) {
    myAbstractDefinition = abstractDef;
    myHasErrors = true;
  }

  public String getName() {
    return myAbstractDefinition.getName();
  }

  public Abstract.Definition getAbstractDefinition() {
    return myAbstractDefinition;
  }

  public DependentLink getParameters() {
    return EmptyDependentLink.getInstance();
  }

  public abstract Type getType(LevelSubstitution polyParams);

  public abstract DefCallExpression getDefCall(LevelSubstitution polyParams);

  public abstract Expression getDefCall(LevelSubstitution polyParams, List<Expression> args);

  public abstract int getNumberOfParameters();

  public boolean typeHasErrors() {
    return myHasErrors;
  }

  public ClassDefinition getThisClass() {
    return myThisClass;
  }

  public Type getTypeWithThis(LevelSubstitution polyParams) {
    return getType(polyParams);
  }

  public void setThisClass(ClassDefinition enclosingClass) {
    myThisClass = enclosingClass;
  }

  public void setPolyParams(List<Binding> params) {
    myPolyParams = params;
  }

  public List<Binding> getPolyParams() {
    return myPolyParams;
  }

  public Binding getPolyParamByType(Definition typeDef) {
    for (Binding binding : myPolyParams) {
      if (binding.getType().toDefCall().getDefinition() == typeDef) {
        return binding;
      }
    }
    return null;
  }

  public boolean isPolymorphic() { return !myPolyParams.isEmpty(); }

  public boolean hasErrors() {
    return myHasErrors;
  }

  public void hasErrors(boolean has) {
    myHasErrors = has;
  }

  public boolean isAbstract() {
    return false;
  }

  public Namespace getOwnNamespace() {
    return EmptyNamespace.INSTANCE;
  }

  public Namespace getInstanceNamespace() {
    return EmptyNamespace.INSTANCE;
  }
}

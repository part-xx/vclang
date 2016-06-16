package com.jetbrains.jetpad.vclang.term.definition;

import com.jetbrains.jetpad.vclang.naming.ResolvedName;
import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.expr.ClassCallExpression;
import com.jetbrains.jetpad.vclang.term.expr.Expression;
import com.jetbrains.jetpad.vclang.term.expr.UniverseExpression;

import java.util.*;

import static com.jetbrains.jetpad.vclang.term.expr.ExpressionFactory.*;

public class ClassDefinition extends Definition {
  private final Map<String, ClassField> myFields = new HashMap<>();
  private Set<ClassDefinition> mySuperClasses = null;

  public ClassDefinition(ResolvedName rn) {
    super(rn, Abstract.Binding.DEFAULT_PRECEDENCE);
    super.hasErrors(false);
  }

  public ClassDefinition(ResolvedName rn, TypeUniverse universe) {
    super(rn, Abstract.Definition.DEFAULT_PRECEDENCE, universe);
    super.hasErrors(false);
  }

  public boolean isSubClassOf(ClassDefinition classDefinition) {
    if (this == classDefinition) {
      return true;
    }
    if (mySuperClasses == null) {
      return false;
    }
    for (ClassDefinition superClass : mySuperClasses) {
      if (superClass.isSubClassOf(classDefinition)) {
        return true;
      }
    }
    return false;
  }

  public void addSuperClass(ClassDefinition superClass) {
    myFields.putAll(superClass.myFields);
    if (mySuperClasses == null) {
      mySuperClasses = new HashSet<>();
    }
    mySuperClasses.add(superClass);
  }

  @Override
  public Expression getType() {
    return new UniverseExpression(getUniverse());
  }

  @Override
  public ClassCallExpression getDefCall() {
    return ClassCall(this, new HashMap<ClassField, ClassCallExpression.ImplementStatement>());
  }

  public ClassField getField(String name) {
    return myFields.get(name);
  }

  public Collection<ClassField> getFields() {
    return myFields.values();
  }

  public int getNumberOfVisibleFields() {
    int result = myFields.size();
    if (getParentField() != null) {
      --result;
    }
    return result;
  }

  public void addField(ClassField field) {
    myFields.put(field.getName(), field);
    field.setThisClass(this);
  }

  public ClassField removeField(String name) {
    return myFields.remove(name);
  }

  public void removeField(ClassField field) {
    myFields.remove(field.getName());
  }

  public ClassField getParentField() {
    return getField("\\parent");
  }

  public void addParentField(ClassDefinition parentClass) {
    setThisClass(parentClass);
    ClassField field = new ClassField(getResolvedName().toNamespace().getChild("\\parent").getResolvedName(), Abstract.Binding.DEFAULT_PRECEDENCE, ClassCall(parentClass), this, param("\\this", ClassCall(this)));
    addField(field);
    getResolvedName().toNamespace().addDefinition(field);
  }

  @Override
  public Expression getTypeWithThis() {
    Expression type = getType();
    if (getThisClass() != null) {
      type = Pi(ClassCall(getThisClass()), type);
    }
    return type;
  }
}

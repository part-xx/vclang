package com.jetbrains.jetpad.vclang.typechecking.implicitargs;

import com.jetbrains.jetpad.vclang.core.expr.type.Type;
import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.typechecking.visitor.CheckTypeVisitor;

public class BaseImplicitArgsInference implements ImplicitArgsInference {
  protected final CheckTypeVisitor myVisitor;

  protected BaseImplicitArgsInference(CheckTypeVisitor visitor) {
    myVisitor = visitor;
  }

  @Override
  public CheckTypeVisitor.TResult infer(Abstract.AppExpression expr, Type expectedType) {
    return null;
  }

  @Override
  public CheckTypeVisitor.TResult infer(Abstract.BinOpExpression expr, Type expectedType) {
    return null;
  }

  @Override
  public CheckTypeVisitor.TResult inferTail(CheckTypeVisitor.TResult fun, Type expectedType, Abstract.Expression expr) {
    return null;
  }
}

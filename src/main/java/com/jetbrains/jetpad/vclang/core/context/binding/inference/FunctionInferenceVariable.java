package com.jetbrains.jetpad.vclang.core.context.binding.inference;

import com.jetbrains.jetpad.vclang.core.definition.Definition;
import com.jetbrains.jetpad.vclang.core.expr.Expression;
import com.jetbrains.jetpad.vclang.core.expr.type.Type;
import com.jetbrains.jetpad.vclang.core.expr.type.TypeMax;
import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.typechecking.error.local.ArgInferenceError;
import com.jetbrains.jetpad.vclang.typechecking.error.local.LocalTypeCheckingError;

public class FunctionInferenceVariable extends InferenceVariable {
  private final int myIndex;
  private final Definition myDefinition;

  public FunctionInferenceVariable(String name, Type type, int index, Definition definition, Abstract.SourceNode sourceNode) {
    super(name, type, sourceNode);
    myIndex = index;
    myDefinition = definition;
  }

  @Override
  public LocalTypeCheckingError getErrorInfer(Expression... candidates) {
    return new ArgInferenceError(ArgInferenceError.functionArg(myIndex, myDefinition != null ? myDefinition.getName() : null), getSourceNode(), candidates);
  }

  @Override
  public LocalTypeCheckingError getErrorMismatch(Type expectedType, TypeMax actualType, Expression candidate) {
    return new ArgInferenceError(ArgInferenceError.functionArg(myIndex, myDefinition != null ? myDefinition.getName() : null), expectedType, actualType, getSourceNode(), candidate);
  }
}

package com.jetbrains.jetpad.vclang.term.context.binding.inference;

import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.expr.Expression;
import com.jetbrains.jetpad.vclang.term.expr.type.Type;
import com.jetbrains.jetpad.vclang.typechecking.error.ArgInferenceError;
import com.jetbrains.jetpad.vclang.typechecking.error.TypeCheckingError;

public class ExpressionInferenceVariable extends InferenceVariable {
  private final Abstract.SourceNode mySourceNode;

  public ExpressionInferenceVariable(Expression type, Abstract.SourceNode sourceNode) {
    super(null, type);
    mySourceNode = sourceNode;
  }

  @Override
  public Abstract.SourceNode getSourceNode() {
    return mySourceNode;
  }

  @Override
  public TypeCheckingError getErrorInfer(Expression... candidates) {
    return new ArgInferenceError(ArgInferenceError.expression(), mySourceNode, candidates);
  }

  @Override
  public TypeCheckingError getErrorMismatch(Expression expectedType, Type actualType, Expression candidate) {
    return new ArgInferenceError(ArgInferenceError.expression(), expectedType, actualType, mySourceNode, candidate);
  }
}
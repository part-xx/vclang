package com.jetbrains.jetpad.vclang.typechecking.implicitargs.equations;

import com.jetbrains.jetpad.vclang.core.expr.InferenceReferenceExpression;

public interface InferenceVariableListener {
  void solved(Equations equations, InferenceReferenceExpression referenceExpression);
}

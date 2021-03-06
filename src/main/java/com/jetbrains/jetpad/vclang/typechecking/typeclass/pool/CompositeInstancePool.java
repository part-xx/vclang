package com.jetbrains.jetpad.vclang.typechecking.typeclass.pool;

import com.jetbrains.jetpad.vclang.core.expr.Expression;
import com.jetbrains.jetpad.vclang.term.Abstract;

import java.util.Arrays;
import java.util.List;

public class CompositeInstancePool implements ClassViewInstancePool {
  private final List<ClassViewInstancePool> myPools;

  public CompositeInstancePool(ClassViewInstancePool... pools) {
    myPools = Arrays.asList(pools);
  }

  @Override
  public Expression getInstance(Abstract.DefCallExpression defCall, Expression classifyingExpression, Abstract.ClassView classView) {
    for (ClassViewInstancePool pool : myPools) {
      Expression expr = pool.getInstance(defCall, classifyingExpression, classView);
      if (expr != null) {
        return expr;
      }
    }
    return null;
  }

  @Override
  public Expression getInstance(Abstract.DefCallExpression defCall, int paramIndex, Expression classifyingExpression, Abstract.ClassDefinition classDefinition) {
    for (ClassViewInstancePool pool : myPools) {
      Expression expr = pool.getInstance(defCall, paramIndex, classifyingExpression, classDefinition);
      if (expr != null) {
        return expr;
      }
    }
    return null;
  }
}

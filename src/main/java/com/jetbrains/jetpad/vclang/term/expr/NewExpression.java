package com.jetbrains.jetpad.vclang.term.expr;

import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.expr.visitor.AbstractExpressionVisitor;
import com.jetbrains.jetpad.vclang.term.expr.visitor.ExpressionVisitor;

import java.util.List;

public class NewExpression extends Expression implements Abstract.NewExpression {
  private final Expression myExpression;

  public NewExpression(Expression expression) {
    myExpression = expression;
  }

  @Override
  public Expression getExpression() {
    return myExpression;
  }

  @Override
  public <T> T accept(ExpressionVisitor<? extends T> visitor) {
    return visitor.visitNew(this);
  }

  @Override
  public Expression getType(List<Expression> context) {
    return myExpression;
  }

  @Override
  public <P, R> R accept(AbstractExpressionVisitor<? super P, ? extends R> visitor, P params) {
    return visitor.visitNew(this, params);
  }
}
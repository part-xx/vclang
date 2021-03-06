package com.jetbrains.jetpad.vclang.core.expr;

import com.jetbrains.jetpad.vclang.core.definition.ClassField;
import com.jetbrains.jetpad.vclang.core.expr.visitor.ExpressionVisitor;
import com.jetbrains.jetpad.vclang.core.sort.LevelArguments;

import java.util.Collections;
import java.util.List;

import static com.jetbrains.jetpad.vclang.core.expr.ExpressionFactory.FieldCall;

public class FieldCallExpression extends DefCallExpression {
  private Expression myExpression;

  public FieldCallExpression(ClassField definition, Expression expression) {
    super(definition, new LevelArguments());
    myExpression = expression;
  }

  public Expression getExpression() {
    return myExpression;
  }

  @Override
  public List<? extends Expression> getDefCallArguments() {
    return Collections.singletonList(myExpression);
  }

  @Override
  public Expression applyThis(Expression thisExpr) {
    assert myExpression == null;
    return FieldCall(getDefinition(), thisExpr);
  }

  @Override
  public ClassField getDefinition() {
    return (ClassField) super.getDefinition();
  }

  @Override
  public <P, R> R accept(ExpressionVisitor<? super P, ? extends R> visitor, P params) {
    return visitor.visitFieldCall(this, params);
  }

  @Override
  public FieldCallExpression toFieldCall() {
    return this;
  }

  @Override
  public Expression getStuckExpression() {
    return myExpression.getStuckExpression();
  }
}

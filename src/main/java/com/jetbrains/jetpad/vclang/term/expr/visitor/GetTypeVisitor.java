package com.jetbrains.jetpad.vclang.term.expr.visitor;

import com.jetbrains.jetpad.vclang.term.context.binding.Binding;
import com.jetbrains.jetpad.vclang.term.context.param.DependentLink;
import com.jetbrains.jetpad.vclang.term.context.param.EmptyDependentLink;
import com.jetbrains.jetpad.vclang.term.context.param.UntypedDependentLink;
import com.jetbrains.jetpad.vclang.term.expr.*;
import com.jetbrains.jetpad.vclang.term.expr.sort.Sort;
import com.jetbrains.jetpad.vclang.term.expr.sort.SortMax;
import com.jetbrains.jetpad.vclang.term.expr.subst.ExprSubstitution;
import com.jetbrains.jetpad.vclang.term.expr.subst.LevelSubstitution;
import com.jetbrains.jetpad.vclang.term.expr.type.PiUniverseType;
import com.jetbrains.jetpad.vclang.term.expr.type.Type;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.jetbrains.jetpad.vclang.term.expr.ExpressionFactory.params;

public class GetTypeVisitor extends BaseExpressionVisitor<Void, Type> {
  @Override
  public Type visitApp(AppExpression expr, Void params) {
    Type functionType = expr.getFunction().accept(this, null);
    return functionType != null ? functionType.applyExpressions(expr.getArguments()) : null;
  }

  @Override
  public Expression visitDefCall(DefCallExpression expr, Void params) {
    return expr.getDefinition().getType().subst(expr.getPolyParamsSubst());
  }

  @Override
  public Expression visitConCall(ConCallExpression expr, Void params) {
    return visitDefCall(expr, null).applyExpressions(expr.getDataTypeArguments());
  }

  @Override
  public Type visitClassCall(ClassCallExpression expr, Void params) {
    return expr.getSorts().toType();
  }

  @Override
  public Expression visitReference(ReferenceExpression expr, Void params) {
    return expr.getBinding().getType().accept(new SubstVisitor(new ExprSubstitution(), new LevelSubstitution()), null);
  }

  @Override
  public Type visitLam(LamExpression expr, Void ignored) {
    Type bodyType = expr.getBody().accept(this, null);
    if (bodyType instanceof Expression) {
      return new PiExpression(expr.getParameters(), (Expression) bodyType);
    } else
    if (bodyType instanceof PiUniverseType) {
      return new PiUniverseType(params(DependentLink.Helper.clone(expr.getParameters()), ((PiUniverseType) bodyType).getParameters()), ((PiUniverseType) bodyType).getSorts());
    } else {
      return null;
    }
  }

  private SortMax getSorts(Type type) {
    if (type instanceof Expression) {
      UniverseExpression universeType = ((Expression) type).normalize(NormalizeVisitor.Mode.WHNF).toUniverse();
      if (universeType != null) {
        return new SortMax(universeType.getSort());
      }
    } else
    if (type instanceof PiUniverseType) {
      if (!((PiUniverseType) type).getParameters().hasNext()) {
        return ((PiUniverseType) type).getSorts();
      }
    }
    return null;
  }

  private Type visitDependentType(DependentTypeExpression expr) {
    SortMax codomain = null;
    if (expr instanceof PiExpression) {
      codomain = getSorts(((PiExpression) expr).getCodomain().accept(this, null));
      if (codomain == null) {
        return null;
      }

      if (codomain.getHLevel().isMinimum()) {
        return new UniverseExpression(Sort.PROP);
      }
    }

    SortMax sorts = new SortMax();
    for (DependentLink link = expr.getParameters(); link.hasNext(); link = link.getNext()) {
      if (!(link instanceof UntypedDependentLink)) {
        SortMax sorts1 = getSorts(link.getType().accept(this, null));
        if (sorts1 == null) {
          return null;
        }
        sorts.add(sorts1);
      }
    }

    if (codomain != null) {
      sorts = new SortMax(sorts.getPLevel().max(codomain.getPLevel()), codomain.getHLevel());
    }

    return new PiUniverseType(EmptyDependentLink.getInstance(), sorts);
  }

  @Override
  public Type visitPi(PiExpression expr, Void params) {
    return visitDependentType(expr);
  }

  @Override
  public Type visitSigma(SigmaExpression expr, Void params) {
    return visitDependentType(expr);
  }

  @Override
  public Expression visitUniverse(UniverseExpression expr, Void params) {
    return new UniverseExpression(expr.getSort().succ());
  }

  @Override
  public Expression visitError(ErrorExpression expr, Void params) {
    Expression expr1 = null;
    if (expr.getExpr() != null) {
      Type type = expr.getExpr().accept(this, null);
      if (type instanceof Expression) {
        expr1 = (Expression) type;
      }
    }
    return new ErrorExpression(expr1, expr.getError());
  }

  @Override
  public Expression visitTuple(TupleExpression expr, Void params) {
    return expr.getType();
  }

  @Override
  public Expression visitProj(ProjExpression expr, Void ignored) {
    Type type = expr.getExpression().accept(this, null);
    if (!(type instanceof Expression)) {
      return null;
    }

    SigmaExpression sigma = ((Expression) type).normalize(NormalizeVisitor.Mode.WHNF).toSigma();
    if (sigma == null) return null;
    DependentLink params = sigma.getParameters();
    if (expr.getField() == 0) {
      return params.getType();
    }

    ExprSubstitution subst = new ExprSubstitution();
    for (int i = 0; i < expr.getField(); i++) {
      if (!params.hasNext()) {
        return null;
      }
      subst.add(params, new ProjExpression(expr.getExpression(), i));
      params = params.getNext();
    }
    return params.getType().subst(subst);
  }

  @Override
  public Expression visitNew(NewExpression expr, Void params) {
    return expr.getExpression();
  }

  private <T extends Binding> List<T> getBindingsFreeIn(List<T> bindings, Expression expr) {
    List<T> result = Collections.emptyList();
    for (T binding : bindings) {
      if (expr.findBinding(binding)) {
        if (result.isEmpty()) {
          result = new ArrayList<>(bindings.size());
        }
        result.add(binding);
      }
    }
    return result;
  }

  @Override
  public Type visitLet(LetExpression expr, Void params) {
    Type type = expr.getExpression().accept(this, null);
    if (type instanceof Expression) {
      return new LetExpression(expr.getClauses(), (Expression) type);
    } else
    if (type instanceof PiUniverseType) {
      for (DependentLink link = ((PiUniverseType) type).getParameters(); link.hasNext(); link = link.getNext()) {
        List<LetClause> clauses = getBindingsFreeIn(expr.getClauses(), link.getType());
        link.setType(clauses.isEmpty() ? link.getType() : new LetExpression(clauses, link.getType()));
      }
      return type;
    } else {
      return null;
    }
  }

  @Override
  public Expression visitOfType(OfTypeExpression expr, Void params) {
    return expr.getType();
  }
}

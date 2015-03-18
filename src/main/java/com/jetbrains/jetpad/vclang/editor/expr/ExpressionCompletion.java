package com.jetbrains.jetpad.vclang.editor.expr;

import com.jetbrains.jetpad.vclang.editor.util.IdCompletionItem;
import com.jetbrains.jetpad.vclang.model.Node;
import jetbrains.jetpad.completion.CompletionItem;
import jetbrains.jetpad.completion.CompletionParameters;
import jetbrains.jetpad.completion.SimpleCompletionItem;
import jetbrains.jetpad.mapper.Mapper;
import jetbrains.jetpad.projectional.generic.Role;
import jetbrains.jetpad.projectional.generic.RoleCompletion;

import java.util.ArrayList;
import java.util.List;

import static com.jetbrains.jetpad.vclang.model.expr.Model.*;

public class ExpressionCompletion implements RoleCompletion<Node, Expression> {
  private static final ExpressionCompletion INSTANCE = new ExpressionCompletion();

  private ExpressionCompletion() {}

  @Override
  public List<CompletionItem> createRoleCompletion(CompletionParameters cp, Mapper<?, ?> mapper, Node node, final Role<Expression> target) {
    List<CompletionItem> result = new ArrayList<>();
    if (!cp.isMenu()) {
      result.add(new IdCompletionItem() {
        @Override
        public Runnable complete(String text) {
          VarExpression expr = new VarExpression();
          expr.name().set(text);
          return target.set(expr);
        }
      });
    }
    result.add(new SimpleCompletionItem("\\lam ", "lambda") {
      @Override
      public Runnable complete(String text) {
        return target.set(new LamExpression());
      }
    });
    result.add(new SimpleCompletionItem("\\app ", "application") {
      @Override
      public Runnable complete(String text) {
        return target.set(new AppExpression());
      }
    });
    result.add(new SimpleCompletionItem("\\zero ", "0") {
      @Override
      public Runnable complete(String text) {
        return target.set(new ZeroExpression());
      }
    });
    result.add(new SimpleCompletionItem("\\N ", "nat") {
      @Override
      public Runnable complete(String s) {
        return target.set(new NatExpression());
      }
    });
    result.add(new SimpleCompletionItem("\\N-elim ", "nat-elim") {
      @Override
      public Runnable complete(String s) {
        return target.set(new NelimExpression());
      }
    });
    result.add(new SimpleCompletionItem("\\S ", "suc") {
      @Override
      public Runnable complete(String s) {
        return target.set(new SucExpression());
      }
    });
    result.add(new SimpleCompletionItem("\\Type ", "Type") {
      @Override
      public Runnable complete(String s) {
        return target.set(new UniverseExpression());
      }
    });
    result.add(new SimpleCompletionItem("\\pi ", "pi") {
      @Override
      public Runnable complete(String s) {
        return target.set(new PiExpression());
      }
    });
    return result;
  }

  public static ExpressionCompletion getInstance() {
    return INSTANCE;
  }
}
package com.jetbrains.jetpad.vclang.term.expr.visitor;

import com.jetbrains.jetpad.vclang.term.prettyprint.PrettyPrintVisitor;
import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.frontend.Concrete;
import com.jetbrains.jetpad.vclang.frontend.ConcreteExpressionFactory;
import com.jetbrains.jetpad.vclang.core.context.param.DependentLink;
import com.jetbrains.jetpad.vclang.core.expr.Expression;
import com.jetbrains.jetpad.vclang.core.expr.LetClause;
import com.jetbrains.jetpad.vclang.core.expr.LetExpression;
import com.jetbrains.jetpad.vclang.core.pattern.elimtree.EmptyElimTreeNode;
import com.jetbrains.jetpad.vclang.typechecking.TypeCheckingTestCase;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.jetbrains.jetpad.vclang.frontend.ConcreteExpressionFactory.*;
import static com.jetbrains.jetpad.vclang.core.expr.ExpressionFactory.*;
import static org.junit.Assert.assertNotNull;

public class PrettyPrintingTest extends TypeCheckingTestCase {
  @Test
  public void prettyPrintingLam() {
    // \x. x x
    DependentLink x = param("x", Pi(Nat(), Nat()));
    Expression expr = Lam(x, Apps(Reference(x), Reference(x)));
    expr.prettyPrint(new StringBuilder(), new ArrayList<String>(), Abstract.Expression.PREC, 0);
  }

  @Test
  public void prettyPrintingLam2() {
    // \x. x (\y. y x) (\z w. x w z)
    DependentLink x = param("x", Pi(Nat(), Pi(Nat(), Nat())));
    DependentLink y = param("y", Pi(Nat(), Nat()));
    DependentLink z = param("z", Nat());
    DependentLink w = param("w", Nat());
    Expression expr = Lam(x, Apps(Reference(x), Lam(y, Apps(Reference(y), Reference(x))), Lam(params(z, w), Apps(Reference(x), Reference(w), Reference(z)))));
    expr.prettyPrint(new StringBuilder(), new ArrayList<String>(), Abstract.Expression.PREC, 0);
  }

  @Test
  public void prettyPrintingU() {
    // (X : Type0) -> X -> X
    DependentLink X = param("x", Universe(0));
    Expression expr = Pi(X, Pi(param(Reference(X)), Reference(X)));
    expr.prettyPrint(new StringBuilder(), new ArrayList<String>(), Abstract.Expression.PREC, 0);
  }

  @Test
  public void prettyPrintingPi() {
    // (t : Nat -> Nat -> Nat) (x y : Nat) (z w : Nat -> Nat) -> ((s : Nat) -> t (z s) (w x)) -> Nat
    DependentLink t = param("t", Pi(Nat(), Pi(Nat(), Nat())));
    DependentLink x = param("x", Nat());
    DependentLink y = param("y", Nat());
    DependentLink z = param("z", Pi(param(Nat()), Nat()));
    DependentLink w = param("w", Pi(param(Nat()), Nat()));
    DependentLink s = param("s", Nat());
    Expression expr = Pi(params(t, x, y, z, w), Pi(param(Pi(s, Apps(Reference(t), Apps(Reference(z), Reference(s)), Apps(Reference(w), Reference(x))))), Nat()));
    expr.prettyPrint(new StringBuilder(), new ArrayList<String>(), Abstract.Expression.PREC, 0);
  }

  @Test
  public void prettyPrintingFunDef() {
    // f (X : Type0) (x : X) : X => x;
    List<Concrete.Argument> arguments = new ArrayList<>(2);
    arguments.add(cTele(cvars("X"), cUniverse(0)));
    arguments.add(cTele(cvars("x"), cVar("X")));
    Concrete.FunctionDefinition def = new Concrete.FunctionDefinition(ConcreteExpressionFactory.POSITION, "f", Abstract.Precedence.DEFAULT, arguments, cVar("X"), Abstract.Definition.Arrow.RIGHT, cLam("X", cLam("x", cVar("x"))), Collections.<Concrete.Statement>emptyList());
    def.accept(new PrettyPrintVisitor(new StringBuilder(), 0), null);
  }

  @Test
  public void prettyPrintingLet() {
    // \let x {A : Type0} (y ; A) : A => y \in x Zero()
    DependentLink A = param("A", Universe(0));
    DependentLink y = param("y", Reference(A));
    LetClause clause = let("x", params(A, y), Reference(A), Reference(y));
    LetExpression expr = Let(lets(clause), Apps(Reference(clause), Zero()));
    expr.prettyPrint(new StringBuilder(), new ArrayList<String>(), Abstract.Expression.PREC, 0);
  }

  @Test
  public void prettyPrintingLetEmpty() {
    // \let x {A : Type0} (y ; A) : A <= \elim y
    DependentLink A = param("A", Universe(0));
    DependentLink y = param("y", Reference(A));
    LetClause clause = let("x", params(A, y), Reference(A), EmptyElimTreeNode.getInstance());
    LetExpression expr = Let(lets(clause), Apps(Reference(clause), Zero()));
    expr.prettyPrint(new StringBuilder(), new ArrayList<String>(), Abstract.Expression.PREC, 0);
  }

  @Test
  public void prettyPrintingPatternDataDef() {
    Concrete.Definition def = parseDef("\\data LE (n m : Nat) | LE (zero) m => LE-zero | LE (suc n) (suc m) => LE-suc (LE n m)");
    assertNotNull(def);
    def.accept(new PrettyPrintVisitor(new StringBuilder(), Abstract.Expression.PREC), null);
  }

  @Test
  public void prettyPrintingDataWithConditions() {
    Concrete.Definition def = parseDef("\\data Z | neg Nat | pos Nat \\with | pos zero => neg zero");
    assertNotNull(def);
    def.accept(new PrettyPrintVisitor(new StringBuilder(), Abstract.Expression.PREC), null);
  }
}

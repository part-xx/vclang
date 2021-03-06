package com.jetbrains.jetpad.vclang.typechecking.constructions;

import com.jetbrains.jetpad.vclang.typechecking.TypeCheckingTestCase;
import com.jetbrains.jetpad.vclang.typechecking.visitor.CheckTypeVisitor;
import org.junit.Test;

import static com.jetbrains.jetpad.vclang.core.expr.ExpressionFactory.*;
import static com.jetbrains.jetpad.vclang.core.expr.ExpressionFactory.Pi;
import static com.jetbrains.jetpad.vclang.core.expr.ExpressionFactory.Universe;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class Lambda extends TypeCheckingTestCase {
  @Test
  public void id() {
    CheckTypeVisitor.Result result = typeCheckExpr("\\lam x => x", Pi(Nat(), Nat()));
    assertNotNull(result);
  }

  @Test
  public void idTyped() {
    CheckTypeVisitor.Result result = typeCheckExpr("\\lam (x : Nat) => x", null);
    assertNotNull(result);
  }

  @Test
  public void constant() {
    CheckTypeVisitor.Result result = typeCheckExpr("\\lam x y => x", Pi(Nat(), Pi(Nat(), Nat())));
    assertNotNull(result);
  }

  @Test
  public void constantSep() {
    CheckTypeVisitor.Result result = typeCheckExpr("\\lam x => \\lam y => x", Pi(param(true, vars("x", "y"), Nat()), Nat()));
    assertNotNull(result);
  }

  @Test
  public void constantTyped() {
    CheckTypeVisitor.Result result = typeCheckExpr("\\lam (x y : Nat) => x", null);
    assertNotNull(result);
  }

  @Test
  public void idImplicit() {
    CheckTypeVisitor.Result result = typeCheckExpr("\\lam {x} => x", Pi(param(false, (String) null, Nat()), Nat()));
    assertNotNull(result);
  }

  @Test
  public void idImplicitError() {
    typeCheckExpr("\\lam {x} => x", Pi(Nat(), Nat()), 1);
  }

  @Test
  public void constantImplicitError() {
    typeCheckExpr("\\lam x {y} => x", Pi(Nat(), Pi(Nat(), Nat())), 1);
  }

  @Test
  public void constantImplicitTeleError() {
    typeCheckExpr("\\lam x {y} => x", Pi(param(true, vars("x", "y"), Nat()), Nat()), 1);
  }

  @Test
  public void constantImplicitTypeError() {
    typeCheckExpr("\\lam x y => x", Pi(Nat(), Pi(param(false, (String) null, Nat()), Nat())), 1);
  }

  @Test
  public void lambdaUniverse() {
    CheckTypeVisitor.Result result = typeCheckExpr("\\lam (x : \\Type1 -> \\Type2) (y : \\Type0) => x y", null);
    assertEquals(result.type, Pi(params(param(Pi(Universe(1), Universe(2))), param(Universe(0))), Universe(2)));
  }
}

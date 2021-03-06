package com.jetbrains.jetpad.vclang.typechecking.constructions;


import com.jetbrains.jetpad.vclang.typechecking.TypeCheckingTestCase;
import org.junit.Test;

public class TypeOmega extends TypeCheckingTestCase {
  @Test
  public void dataExpansion() {
    typeCheckClass(
      "\\data D (A : \\Type) (a : A) | d (B : A -> \\Type2)\n" +
      "\\function f : \\Pi {A : \\Type \\lp} {a : A} -> (A -> \\Type1) -> D A a => d\n" +
      "\\function test => f {_} {\\Set0} {\\Prop} (\\lam _ => \\Type0)");
  }

  @Test
  public void notAllowedInTypeArgs() {
    typeCheckClass(
      "\\function f (A : \\Type -> \\Type) => 0\n" +
      "\\function g : \\Type -> \\Type => \\lam X => X", 3);
  }

  @Test
  public void notAllowedAsExpression() {
    typeCheckClass("\\function f => \\Type", 1);
  }

  @Test
  public void equalityOfTypes() {
    typeCheckClass("\\function f (A B : \\Type) => A = B");
  }

  @Test
  public void callPolyFromOmega() {
     typeCheckClass(
         "\\function f (A : \\Type \\lp) => A\n" +
         "\\function g (A : \\Type) => f A");
  }

  @Test
  public void typeOmegaResult() {
    typeCheckClass("\\function f (A : \\Type \\lp) : \\Type => A");
  }

  @Test
  public void callNonPolyFromOmega() {
    typeCheckClass(
        "\\function f (A : \\Type0) => 0\n" +
        "\\function g (A : \\Type) => f A", 1);
  }
}

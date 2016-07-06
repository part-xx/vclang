package com.jetbrains.jetpad.vclang.record;

import org.junit.Test;

import static com.jetbrains.jetpad.vclang.naming.NameResolverTestCase.resolveNamesClass;
import static com.jetbrains.jetpad.vclang.typechecking.TypeCheckingTestCase.typeCheckClass;

public class ImplementTest {
  @Test
  public void implement() {
    typeCheckClass(
        "\\class A {\n" +
        "  \\abstract a : Nat\n" +
        "}\n" +
        "\\class B \\extends A {\n" +
        "  \\implement a => 0\n" +
        "}\n" +
        "\\function f (b : B) : b.a = 0 => path (\\lam _ => 0)");
  }

  @Test
  public void implementInFunctionError() {
    resolveNamesClass("test",
        "\\class X {\n" +
        "  \\abstract x : Nat\n" +
        "  \\static \\function f => 0\n" +
        "    \\where\n" +
        "      \\implement x => 1\n" +
        "}", 1);
  }

  @Test
  public void implementUnknownError() {
    resolveNamesClass("test",
        "\\class A {\n" +
        "  \\abstract a : Nat\n" +
        "}\n" +
        "\\class B \\extends A {\n" +
        "  \\implement b => 0\n" +
        "}", 1);
  }

  @Test
  public void implementTypeMismatchError() {
    typeCheckClass(
        "\\class A {\n" +
        "  \\abstract a : Nat -> Nat\n" +
        "}\n" +
        "\\class B \\extends A {\n" +
        "  \\implement a => 0\n" +
        "}", 1);
  }

  @Test
  public void implement2() {
    typeCheckClass(
        "\\class A {\n" +
        "  \\abstract A : \\Set0\n" +
        "  \\abstract a : A\n" +
        "}\n" +
        "\\class B \\extends A {\n" +
        "  \\implement A => Nat\n" +
        "  \\implement a => 0\n" +
        "}\n" +
        "\\function f (b : B) : b.a = 0 => path (\\lam _ => 0)");
  }

  @Test
  public void implement3() {
    typeCheckClass(
        "\\static \\class A {\n" +
        "  \\abstract a : Nat\n" +
        "}\n" +
        "\\static \\class B \\extends A {\n" +
        "  \\implement a => 0\n" +
        "}\n" +
        "\\function f (x : A) => x.a\n" +
        "\\function g (b : B) : f b = 0 => path (\\lam _ => 0)");
  }

  @Test
  public void implementImplementedError() {
    typeCheckClass(
        "\\static \\class A {\n" +
        "  \\abstract a : Nat\n" +
        "}\n" +
        "\\static \\class B \\extends A {\n" +
        "  \\implement a => 0\n" +
        "}\n" +
        "\\static \\class C \\extends B {\n" +
        "  \\implement a => 0\n" +
        "}", 1);
  }

  @Test
  public void implementNew() {
    typeCheckClass(
        "\\static \\class A {\n" +
        "  \\abstract A : \\Set0\n" +
        "  \\abstract a : A\n" +
        "}\n" +
        "\\static \\class B \\extends A {\n" +
        "  \\implement A => Nat\n" +
        "}\n" +
        "\\function f (x : A) => x.a\n" +
        "\\function g : f (\\new B { a => 0 }) = 0 => path (\\lam _ => 0)");
  }

  @Test
  public void implementNewError() {
    typeCheckClass(
        "\\static \\class A {\n" +
        "  \\abstract a : Nat\n" +
        "}\n" +
        "\\static \\class B \\extends A {\n" +
        "  \\implement a => 0\n" +
        "}\n" +
        "\\function f => \\new B { a => 1 }", 1);
  }

  @Test
  public void implementMultiple() {
    typeCheckClass(
        "\\static \\class A {\n" +
        "  \\abstract a : Nat\n" +
        "  \\abstract b : Nat\n" +
        "  \\abstract c : Nat\n" +
        "}\n" +
        "\\static \\class B \\extends A {\n" +
        "  \\implement b => 0\n" +
        "}\n" +
        "\\static \\class C \\extends A {\n" +
        "  \\implement c => 0\n" +
        "}\n" +
        "\\static \\class D \\extends B, C {\n" +
        "  \\abstract p : b = c\n" +
        "  \\abstract f : \\Pi (q : 0 = 0 -> \\Set0) -> q p -> Nat\n" +
        "}\n" +
        "\\function f => \\new D { a => 1 | p => path (\\lam _ => 0) | f => \\lam _ _ => 0 }");
  }

  @Test
  public void implementMultipleSame() {
    typeCheckClass(
        "\\static \\class A {\n" +
        "  \\abstract a : Nat\n" +
        "  \\abstract b : Nat\n" +
        "  \\abstract c : Nat\n" +
        "}\n" +
        "\\static \\class B \\extends A {\n" +
        "  \\implement b => a\n" +
        "}\n" +
        "\\static \\class C \\extends A {\n" +
        "  \\implement b => a\n" +
        "}\n" +
        "\\static \\class D \\extends B, C {\n" +
        "  \\implement a => 1\n" +
        "}\n" +
        "\\function f => \\new D { c => 2 }");
  }

  @Test
  public void implementMultipleSameError() {
    typeCheckClass(
        "\\static \\class A {\n" +
        "  \\abstract a : Nat\n" +
        "}\n" +
        "\\static \\class B \\extends A {\n" +
        "  \\implement a => 0\n" +
        "}\n" +
        "\\static \\class C \\extends A {\n" +
        "  \\implement a => 1\n" +
        "}\n" +
        "\\static \\class D \\extends B, C {}", 1);
  }

  @Test
  public void implementRenamed() {
    typeCheckClass(
        "\\class A {\n" +
        "  \\abstract A : \\Set0\n" +
        "  \\abstract a : A\n" +
        "}\n" +
        "\\class B \\extends A \\renaming A \\to A', a \\to a' {\n" +
        "  \\implement A => Nat\n" +
        "}\n" +
        "\\class C \\extends B {" +
        "  \\implement a' => 0\n" +
        "}\n" +
        "\\function f (c : C) : c.a' = 0 => path (\\lam _ => 0)\n" +
        "\\function g => f (\\new C)");
  }

  @Test
  public void implementHidden() {
    typeCheckClass(
        "\\class A {\n" +
        "  \\abstract A : \\Set0\n" +
        "  \\abstract a : A\n" +
        "}\n" +
        "\\class B \\extends A \\hiding A {\n" +
        "  \\implement A => Nat\n" +
        "}\n" +
        "\\function f => \\new B { a => 0 }");
  }

  @Test
  public void implementHidden2() {
    resolveNamesClass("test",
        "\\class A {\n" +
        "  \\abstract A : \\Set0\n" +
        "  \\abstract a : A\n" +
        "}\n" +
        "\\class B \\extends A \\hiding A {}\n" +
        "\\class C \\extends B {\n" +
        "  \\implement A => Nat\n" +
        "}\n" +
        "\\function f => \\new C { a => 0 }", 1);
  }

  // TODO: Add tests on the universe of a class
}
package com.jetbrains.jetpad.vclang.classes;

import com.jetbrains.jetpad.vclang.module.DummyOutputSupplier;
import com.jetbrains.jetpad.vclang.module.DummySourceSupplier;
import com.jetbrains.jetpad.vclang.module.ModuleLoader;
import com.jetbrains.jetpad.vclang.term.definition.ClassDefinition;
import org.junit.Test;

import static com.jetbrains.jetpad.vclang.parser.ParserTestCase.parseDefs;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class MultipleInheritanceTest {
  @Test
  public void multipleInheritanceTest() {
    ModuleLoader moduleLoader = new ModuleLoader();
    moduleLoader.init(DummySourceSupplier.getInstance(), DummyOutputSupplier.getInstance(), false);
    String text =
        "\\function (+) (x y : Nat) : Nat" +
        "\\class A { \\function x => 0 }\n" +
        "\\class B { \\function y : Nat }\n" +
        "\\class C \\extends A, B { \\function z => x + y }\n" +
        "\\function f : C.x = (\\new C { \\override y => 1 }).x => path (\\lam _ => 0)\n" +
        "\\function g (c : C) : c.z = (\\new C { \\override y => c.y }).z => path (\\lam _ => 0 + c.y)";
    parseDefs(moduleLoader, text);
  }

  @Test
  public void commonFunctionTest() {
    ModuleLoader moduleLoader = new ModuleLoader();
    moduleLoader.init(DummySourceSupplier.getInstance(), DummyOutputSupplier.getInstance(), false);
    String text =
        "\\class A { \\function x : Nat }\n" +
        "\\class B { \\function x => 0 }\n" +
        "\\class C \\extends A, B { }\n" +
        "\\function f : C.x = (\\new C).x => path (\\lam _ => 0)";
    ClassDefinition result = parseDefs(moduleLoader, text);
    assertTrue(result.getPublicFields().get(2) instanceof ClassDefinition);
    ClassDefinition classC = (ClassDefinition) result.getPublicFields().get(2);
    assertEquals(1, classC.getPublicFields().size());
    assertEquals(1, classC.getStaticFields().size());
  }

  @Test
  public void commonFunctionTest2() {
    ModuleLoader moduleLoader = new ModuleLoader();
    moduleLoader.init(DummySourceSupplier.getInstance(), DummyOutputSupplier.getInstance(), false);
    String text =
        "\\class A { \\function x => 0 \\function y => x }\n" +
        "\\class B { \\function y => 0 }\n" +
        "\\class C \\extends A, B { }\n" +
        "\\function f : C.x = (\\new C).y => path (\\lam _ => A.y)";
    ClassDefinition result = parseDefs(moduleLoader, text);
    assertTrue(result.getPublicFields().get(2) instanceof ClassDefinition);
    ClassDefinition classC = (ClassDefinition) result.getPublicFields().get(2);
    assertEquals(2, classC.getPublicFields().size());
    assertEquals(2, classC.getStaticFields().size());
  }

  @Test
  public void commonFunctionTypeTestError() {
    ModuleLoader moduleLoader = new ModuleLoader();
    moduleLoader.init(DummySourceSupplier.getInstance(), DummyOutputSupplier.getInstance(), false);
    String text =
        "\\class A { \\function x : Nat }\n" +
        "\\class B { \\function x (y : Nat) => 0 }\n" +
        "\\class C \\extends A, B { }\n";
    parseDefs(moduleLoader, text, 1);
  }

  @Test
  public void commonFunctionTermTestError() {
    ModuleLoader moduleLoader = new ModuleLoader();
    moduleLoader.init(DummySourceSupplier.getInstance(), DummyOutputSupplier.getInstance(), false);
    String text =
        "\\class A { \\function x => 1 \\function y => x }\n" +
            "\\class B { \\function y => 0 }\n" +
            "\\class C \\extends A, B { }";
    parseDefs(moduleLoader, text, 1);
  }

  @Test
  public void commonDataTest() {
    ModuleLoader moduleLoader = new ModuleLoader();
    moduleLoader.init(DummySourceSupplier.getInstance(), DummyOutputSupplier.getInstance(), false);
    String text =
        "\\class A { \\data D (A : \\Type0) | con A \\function f (x : Nat) => con x }\n" +
        "\\class B { \\data D (A : \\Type0) | con A \\function g : Nat -> D Nat }\n" +
        "\\class C \\extends A, B { \\override g => f }";
    parseDefs(moduleLoader, text);
  }

  @Test
  public void commonDataTest2() {
    ModuleLoader moduleLoader = new ModuleLoader();
    moduleLoader.init(DummySourceSupplier.getInstance(), DummyOutputSupplier.getInstance(), false);
    String text =
        "\\class A { \\data D | con }\n" +
        "\\class A1 \\extends A { \\function f => con }\n" +
        "\\class B { \\data D | con \\function f : D }\n" +
        "\\class C \\extends A, B { }";
    parseDefs(moduleLoader, text);
  }

  @Test
  public void typeIntersectionTest() {
    ModuleLoader moduleLoader = new ModuleLoader();
    moduleLoader.init(DummySourceSupplier.getInstance(), DummyOutputSupplier.getInstance(), false);
    String text =
        "\\class A { \\function x : \\Type0 }\n" +
        "\\class B { \\function x : \\Type1 }\n" +
        "\\class C \\extends A, B { \\function f (y : \\Type0) => 0 }\n" +
        "\\function f (c : C) => C.f c.x ";
    ClassDefinition result = parseDefs(moduleLoader, text);
    assertTrue(result.getPublicFields().get(2) instanceof ClassDefinition);
    ClassDefinition classC = (ClassDefinition) result.getPublicFields().get(2);
    assertEquals(1, classC.getPublicFields().size());
    assertEquals(1, classC.getStaticFields().size());
  }

  @Test
  public void commonFunctionIndirectTest() {
    ModuleLoader moduleLoader = new ModuleLoader();
    moduleLoader.init(DummySourceSupplier.getInstance(), DummyOutputSupplier.getInstance(), false);
    String text =
        "\\class A { \\function T : \\Type0 \\function f : T -> T \\function h (x : T) : f x = x }\n" +
        "\\class B { \\function T : \\Type1 \\function g : T -> T \\function h (x : T) : g x = x }\n" +
        "\\class C \\extends A, B { \\override f => g }";
    parseDefs(moduleLoader, text);
  }

  @Test
  public void commonFunctionFixTestError() {
    ModuleLoader moduleLoader = new ModuleLoader();
    moduleLoader.init(DummySourceSupplier.getInstance(), DummyOutputSupplier.getInstance(), false);
    String text =
        "\\class A { \\function T : \\Type0 \\function f : T -> T \\function h (x : T) : f x = x }\n" +
        "\\class B { \\function T : \\Type1 \\function g : T -> T \\function h (x : T) : g x = x }\n" +
        "\\class C \\extends A, B { \\override h \\as Ah }";
    parseDefs(moduleLoader, text, 1);
  }

  @Test
  public void commonFunctionFixTest() {
    ModuleLoader moduleLoader = new ModuleLoader();
    moduleLoader.init(DummySourceSupplier.getInstance(), DummyOutputSupplier.getInstance(), false);
    String text =
        "\\class A { \\function T : \\Type0 \\function f : T -> T \\function h (x : T) : f x = x }\n" +
        "\\class B { \\function T : \\Type1 \\function g : T -> T \\function h (x : T) : g x = x }\n" +
        "\\class C \\extends A, B { \\override A.h \\as Ah }";
    parseDefs(moduleLoader, text);
  }

  @Test
  public void commonFunctionFixTestError2() {
    ModuleLoader moduleLoader = new ModuleLoader();
    moduleLoader.init(DummySourceSupplier.getInstance(), DummyOutputSupplier.getInstance(), false);
    String text =
        "\\class A { \\function T : \\Type0 \\function f : T -> T \\function h (x : T) : f x = x }\n" +
        "\\class B { \\function T : \\Type1 \\function g : T -> T \\function h (x : T) : g x = x }\n" +
        "\\class C \\extends A, B { \\override A.h \\as g }";
    parseDefs(moduleLoader, text, 1);
  }

  @Test
  public void commonAncestorTestError() {
    ModuleLoader moduleLoader = new ModuleLoader();
    moduleLoader.init(DummySourceSupplier.getInstance(), DummyOutputSupplier.getInstance(), false);
    String text =
        "\\class A { }\n" +
        "\\class B { }\n" +
        "\\class C \\extends A, B { }\n" +
        "\\class D \\extends A, B { }\n" +
        "\\class E { \\function x : C }\n" +
        "\\class F { \\function x : D }\n" +
        "\\class G \\extends E, F { }";
    parseDefs(moduleLoader, text);
  }

  @Test
  public void commonAncestorTest() {
    ModuleLoader moduleLoader = new ModuleLoader();
    moduleLoader.init(DummySourceSupplier.getInstance(), DummyOutputSupplier.getInstance(), false);
    String text =
        "\\class A { }\n" +
        "\\class B { }\n" +
        "\\class C \\extends A, B { }\n" +
        "\\class D \\extends C { }\n" +
        "\\class E { \\function x : C }\n" +
        "\\class F { \\function x : D }\n" +
        "\\class G \\extends E, F { }";
    parseDefs(moduleLoader, text);
  }
}

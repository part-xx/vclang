package com.jetbrains.jetpad.vclang.classes;

import com.jetbrains.jetpad.vclang.module.DummyOutputSupplier;
import com.jetbrains.jetpad.vclang.module.DummySourceSupplier;
import com.jetbrains.jetpad.vclang.module.ModuleLoader;
import org.junit.Test;

import static com.jetbrains.jetpad.vclang.parser.ParserTestCase.parseDefs;

public class InheritanceTest {
  @Test
  public void inheritanceTest() {
    ModuleLoader moduleLoader = new ModuleLoader();
    moduleLoader.init(DummySourceSupplier.getInstance(), DummyOutputSupplier.getInstance(), false);
    String text =
        "\\class A { \\function x : Nat \\function z => 0 }\n" +
        "\\class B \\extends A { \\function y : Nat }\n" +
        "\\function f (b : B) : b.z = 0 => path (\\lam _ => 0)\n" +
        "\\function g (b : B) => b.x = b.y";
    parseDefs(moduleLoader, text);
  }

  @Test
  public void doubleInheritanceTest() {
    ModuleLoader moduleLoader = new ModuleLoader();
    moduleLoader.init(DummySourceSupplier.getInstance(), DummyOutputSupplier.getInstance(), false);
    String text =
        "\\function (+) (x y : Nat) : Nat\n" +
        "\\class A { \\function x : Nat \\function z => 0 }\n" +
        "\\class B \\extends A { \\function y : Nat }\n" +
        "\\class C \\extends B { \\function w => x + y + z }\n" +
        "\\function f (c : C) => c.x + c.y + c.z + c.w\n";
    parseDefs(moduleLoader, text);
  }

  @Test
  public void staticEvalTestError() {
    ModuleLoader moduleLoader = new ModuleLoader();
    moduleLoader.init(DummySourceSupplier.getInstance(), DummyOutputSupplier.getInstance(), false);
    String text =
        "\\class A { \\function z => 0 }\n" +
        "\\function f (a : A) : a.z = 0 => path (\\lam _ => 0)\n";
    parseDefs(moduleLoader, text, 1);
  }

  @Test
  public void staticEvalTest() {
    ModuleLoader moduleLoader = new ModuleLoader();
    moduleLoader.init(DummySourceSupplier.getInstance(), DummyOutputSupplier.getInstance(), false);
    String text =
        "\\class A { \\function z => 0 }\n" +
        "\\class B \\extends A { \\function z => 1 }\n" +
        "\\function f (a : A) (_ : a.z = a.z) => 0\n" +
        "\\function g => f (\\new A) (path (\\lam _ => 0))\n" +
        "\\function h => f (\\new B) (path (\\lam _ => 1))\n";
    parseDefs(moduleLoader, text);
  }

  @Test
  public void overrideTest() {
    ModuleLoader moduleLoader = new ModuleLoader();
    moduleLoader.init(DummySourceSupplier.getInstance(), DummyOutputSupplier.getInstance(), false);
    String text =
        "\\class A { \\function x : Nat \\function z => 0 }\n" +
        "\\class B \\extends A { \\override x => z }\n" +
        "\\class C \\extends B { \\override x => suc z }\n" +
        "\\class D \\extends B { \\function y => x }\n" +
        "\\class E \\extends C { \\function y => x }\n" +
        "\\function f (d : D) : d.y = 0 => path (\\lam _ => 0)\n" +
        "\\function g (e : E) : e.y = 1 => path (\\lam _ => 1)\n";
    parseDefs(moduleLoader, text);
  }

  @Test
  public void newTest() {
    ModuleLoader moduleLoader = new ModuleLoader();
    moduleLoader.init(DummySourceSupplier.getInstance(), DummyOutputSupplier.getInstance(), false);
    String text =
        "\\class A { \\function x : Nat \\function z => 0 }\n" +
        "\\class B \\extends A { \\override z => 1 }\n" +
        "\\class C \\extends B { \\override x => z }\n" +
        "\\function f (a : A) => a.x\n" +
        "\\function g : f (\\new C) = 1 => path (\\lam _ => 1)\n";
    parseDefs(moduleLoader, text);
  }

  @Test
  public void newAnonymousTest() {
    ModuleLoader moduleLoader = new ModuleLoader();
    moduleLoader.init(DummySourceSupplier.getInstance(), DummyOutputSupplier.getInstance(), false);
    String text =
        "\\class A { \\function x : Nat \\function z => 0 }\n" +
        "\\class B \\extends A { \\override z => 1 }\n" +
        "\\function f (a : A) => a.x\n" +
        "\\function g : f (\\new B { \\override x => z }) = 1 => path (\\lam _ => 1)\n";
    parseDefs(moduleLoader, text);
  }

  @Test
  public void newComparisonTest() {
    ModuleLoader moduleLoader = new ModuleLoader();
    moduleLoader.init(DummySourceSupplier.getInstance(), DummyOutputSupplier.getInstance(), false);
    String text =
        "\\class A { \\function x : Nat \\function z => 0 }\n" +
        "\\class B \\extends A { \\override z => 1 }\n" +
        "\\class C \\extends A { \\override x => suc z }\n" +
        "\\function f (a : A) => a.x\n" +
        "\\function g : \\new B { \\override x => z } = \\new B { \\override x => 1 } => path (\\lam _ => \\new C)\n";
    parseDefs(moduleLoader, text);
  }

  @Test
  public void mutualRecursionTestError() {
    ModuleLoader moduleLoader = new ModuleLoader();
    moduleLoader.init(DummySourceSupplier.getInstance(), DummyOutputSupplier.getInstance(), false);
    String text =
        "\\class A { \\function x : Nat \\function y => x }\n" +
        "\\class B \\extends A { \\override x => y }\n";
    parseDefs(moduleLoader, text, 1);
  }

  @Test
  public void mutualRecursionTest() {
    ModuleLoader moduleLoader = new ModuleLoader();
    moduleLoader.init(DummySourceSupplier.getInstance(), DummyOutputSupplier.getInstance(), false);
    String text =
        "\\class A { \\function x : Nat \\function y => x }\n" +
        "\\class B \\extends A { \\override x => y \\override y => 0 }\n";
    parseDefs(moduleLoader, text);
  }

  @Test
  public void mutualRecursionTestError2() {
    ModuleLoader moduleLoader = new ModuleLoader();
    moduleLoader.init(DummySourceSupplier.getInstance(), DummyOutputSupplier.getInstance(), false);
    String text =
        "\\class A { \\function x : Nat \\function y => x }\n" +
        "\\class B \\extends A { \\function z => 0 \\override x => z }\n" +
        "\\class C \\extends B { \\override z => y }\n";
    parseDefs(moduleLoader, text, 1);
  }

  @Test
  public void internalInheritanceTestError() {
    ModuleLoader moduleLoader = new ModuleLoader();
    moduleLoader.init(DummySourceSupplier.getInstance(), DummyOutputSupplier.getInstance(), false);
    parseDefs(moduleLoader, "\\class A { \\class B \\extends A { } }", 1);
  }
}

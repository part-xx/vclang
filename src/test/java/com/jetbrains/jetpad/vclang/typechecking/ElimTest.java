package com.jetbrains.jetpad.vclang.typechecking;

import com.jetbrains.jetpad.vclang.term.Prelude;
import com.jetbrains.jetpad.vclang.core.context.binding.Binding;
import com.jetbrains.jetpad.vclang.core.context.binding.TypedBinding;
import com.jetbrains.jetpad.vclang.core.context.param.EmptyDependentLink;
import com.jetbrains.jetpad.vclang.core.definition.Constructor;
import com.jetbrains.jetpad.vclang.core.definition.FunctionDefinition;
import com.jetbrains.jetpad.vclang.core.expr.Expression;
import com.jetbrains.jetpad.vclang.core.sort.LevelArguments;
import com.jetbrains.jetpad.vclang.core.expr.visitor.NormalizeVisitor;
import org.junit.Test;

import java.util.Collections;

import static com.jetbrains.jetpad.vclang.core.expr.ExpressionFactory.*;
import static org.junit.Assert.assertEquals;

public class ElimTest extends TypeCheckingTestCase {
  @Test
  public void elim2() {
    typeCheckClass(
        "\\data D Nat (x y : Nat) | con1 Nat | con2 (Nat -> Nat) (a b c : Nat)\n" +
        "\\function P (a1 b1 c1 : Nat) (d1 : D a1 b1 c1) (a2 b2 c2 : Nat) (d2 : D a2 b2 c2) : \\Type0 <= \\elim d1\n" +
        "  | con2 _ _ _ _ => Nat -> Nat\n" +
        "  | con1 _ => Nat\n" +
        "\\function test (q w : Nat) (e : D w 0 q) (r : D q w 1) : P w 0 q e q w 1 r <= \\elim e, r\n" +
        " | con2 x y z t, con1 s => x\n" +
        " | con1 _, con1 s => s\n" +
        " | con1 s, con2 x y z t => x q\n" +
        " | con2 _ y z t, con2 x y z t => x");
  }

  @Test
  public void elim3() {
    typeCheckClass(
        "\\data D (x : Nat -> Nat) (y : Nat) | con1 {Nat} Nat | con2 (Nat -> Nat) {a b c : Nat}\n" +
        "\\function test (q : Nat -> Nat) (e : D q 0) (r : D (\\lam x => x) (q 1)) : Nat <= \\elim e, r\n" +
        "  | con2 _ {y} {z} {t}, con1 s => q t\n" +
        "  | con1 {z} _, con1 s => z\n" +
        "  | con1 s, con2 y => y s\n" +
        "  | con2 _ {a} {b}, con2 y => y (q b)");
  }

  @Test
  public void elim4() {
    typeCheckClass(
        "\\function test (x : Nat) : Nat <= \\elim x | zero => 0 | _ => 1\n" +
        "\\function test2 (x : Nat) : 1 = 1 => path (\\lam _ => test x)", 1);
  }

  @Test
  public void elim5() {
    typeCheckClass(
        "\\data D (x : Nat) | D zero => d0 | D (suc n) => d1\n" +
        "\\function test (x : D 0) : Nat <= \\elim x | d0 => 0");
  }

  @Test
  public void elimUnknownIndex1() {
    typeCheckClass(
        "\\data D (x : Nat) | D zero => d0 | D (suc _) => d1\n" +
        "\\function test (x : Nat) (y : D x) : Nat <= \\elim y | d0 => 0 | d1 => 1", 2);
  }

  @Test
  public void elimTest() {
    typeCheckClass(
        "\\function test (x : Nat) : Nat => \\case x | zero a => 0 | sucs n => 1", 2);
  }

  @Test
  public void elimUnknownIndex2() {
    typeCheckClass(
        "\\data D (x : Nat) | D zero => d0 | D (suc _) => d1\n" +
        "\\function test (x : Nat) (y : D x) : Nat <= \\elim y | d0 => 0 | _ => 1", 1);
  }

  @Test
  public void elimUnknownIndex3() {
    typeCheckClass(
        "\\data D (x : Nat) | D zero => d0 | D (suc _) => d1\n" +
        "\\function test (x : Nat) (y : D x) : Nat <= \\elim y | _ => 0", 0);
  }

  @Test
  public void elimUnknownIndex4() {
    typeCheckClass(
        "\\data E | A | B | C\n" +
        "\\data D (x : E) | D A => d0 | D B => d1 | D _ => d2\n" +
        "\\function test (x : E) (y : D x) : Nat <= \\elim y | d0 => 0 | d1 => 1", 2);
  }

  @Test
  public void elimUnknownIndex5() {
    typeCheckClass(
        "\\data E | A | B | C\n" +
        "\\data D (x : E) | D A => d0 | D B => d1 | D _ => d2\n" +
        "\\function test (x : E) (y : D x) : Nat <= \\elim y | d0 => 0 | d1 => 1 | d2 => 2", 3);
  }

  @Test
  public void elimUnknownIndex6() {
    typeCheckClass(
        "\\data E | A | B | C\n" +
        "\\data D (x : E) | D A => d0 | D B => d1 | D _ => d2\n" +
        "\\function test (x : E) (y : D x) : Nat <= \\elim y | d0 => 0 | d1 => 1 | _ => 2", 2);
  }

  @Test
  public void elimUnknownIndex7() {
    typeCheckClass(
        "\\data E | A | B | C\n" +
        "\\data D (x : E) | D A => d0 | D B => d1 | D _ => d2\n" +
        "\\function test (x : E) (y : D x) : Nat <= \\elim y | _ => 0", 0);
  }

  @Test
  public void elimTooManyArgs() {
    typeCheckClass("\\data A | a Nat Nat \\function test (a : A) : Nat <= \\elim a | a _ _ _ =>0", 1);
  }

  @Test
  public void elim6() {
    typeCheckClass(
        "\\data D | d Nat Nat\n" +
        "\\function test (x : D) : Nat <= \\elim x | d zero zero => 0 | d (suc _) _ => 1 | d _ (suc _) => 2");
  }

  @Test
  public void elim7() {
    typeCheckClass(
        "\\data D | d Nat Nat\n" +
        "\\function test (x : D) : Nat <= \\elim x | d zero zero => 0 | d (suc (suc _)) zero => 0", 1);
  }

  @Test
  public void elim8() {
    TypeCheckingTestCase.TypeCheckClassResult result = typeCheckClass(
        "\\data D | d Nat Nat\n" +
        "\\function test (x : D) : Nat <= \\elim x | d zero zero => 0 | d _ _ => 1");
    FunctionDefinition test = (FunctionDefinition) result.getDefinition("test");
    Constructor d = (Constructor) result.getDefinition("d");
    Binding binding = new TypedBinding("y", Nat());
    Expression call1 = ConCall(d, new LevelArguments(), Collections.<Expression>emptyList(), Zero(), Reference(binding));
    Expression call2 = ConCall(d, new LevelArguments(), Collections.<Expression>emptyList(), Suc(Zero()), Reference(binding));
    assertEquals(FunCall(test, new LevelArguments(), call1), FunCall(test, new LevelArguments(), call1).normalize(NormalizeVisitor.Mode.NF));
    assertEquals(Suc(Zero()), FunCall(test, new LevelArguments(), call2).normalize(NormalizeVisitor.Mode.NF));
  }

  @Test
  public void elim9() {
    typeCheckClass(
        "\\data D Nat | D (suc n) => d1 | D _ => d | D zero => d0\n" +
        "\\function test (n : Nat) (a : D (suc n)) : Nat <= \\elim a | d => 0", 1);
  }

  @Test
  public void elim10() {
    typeCheckClass("\\data Bool | true | false\n" +
                   "\\function tp : \\Pi (x : Bool) -> \\Type0 => \\lam x => \\case x\n" +
                   "| true => Bool\n" +
                   "| false => Nat\n" +
                   "\\function f (x : Bool) : tp x <= \\elim x\n" +
                   "| true => true\n" +
                   "| false => zero\n");
  }

  @Test
  public void elimEmptyBranch() {
    typeCheckClass(
        "\\data D Nat | D (suc n) => dsuc\n" +
        "\\function test (n : Nat) (d : D n) : Nat <= \\elim n, d | zero, _! | suc n, dsuc => 0");
  }

  @Test
  public void elimEmptyBranchError() {
    typeCheckClass(
        "\\data D Nat | D (suc n) => dsuc\n" +
        "\\function test (n : Nat) (d : D n) : Nat <= \\elim n, d | suc n, _! | zero, _! => 0", 1);
  }

  @Test
  public void elimUnderLetError() {
    typeCheckClass("\\function test (n : Nat) : Nat <= \\let x => 0 \\in \\elim n | _! => 0", 1);
  }

  @Test
  public void elimOutOfDefinitionError() {
    typeCheckClass("\\function test (n : Nat) : Nat <= \\let x : Nat <= \\elim n | _ => 0 \\in 1", 1);
  }

  @Test
  public void elimLetError() {
    typeCheckClass("\\function test => \\let x => 0 \\in \\let y : Nat <= \\elim x | _ => 0 \\in 1", 1);
  }

  @Test
  public void testSide() {
    typeCheckClass("\\function test (n : Nat) <= suc (\\elim n | suc n => n | zero => 0)", 1);
  }

  @Test
  public void testNoPatterns() {
    typeCheckClass("\\function test (n : Nat) : 0 = 1 <= \\elim n", 1);
  }

  @Test
  public void testAuto() {
    typeCheckClass(
        "\\data Empty\n" +
        "\\function test (n : Nat) (e : Empty) : Empty <= \\elim n, e");
  }

  @Test
  public void testAuto1() {
    typeCheckClass(
        "\\data Geq Nat Nat | Geq _ zero => Geq-zero | Geq (suc n) (suc m) => Geq-suc (Geq n m)\n" +
        "\\function test (n m : Nat) (p : Geq n m) : Nat <= \\elim n, m, p\n" +
        "  | _!, zero, Geq-zero => 0\n" +
        "  | suc n, suc m, Geq-suc p => 1");
  }

  @Test
  public void testAutoNonData() {
    typeCheckClass(
        "\\data D Nat | D zero => dcons\n" +
        "\\data E (n : Nat) (Nat -> Nat) (D n) | econs\n" +
        "\\function test (n : Nat) (d : D n) (e : E n (\\lam x => x) d) : Nat <= \\elim n, d, e\n" +
        "  | zero, dcons, econs => 1");
  }

  @Test
  public void testElimNeedNormalize() {
    typeCheckClass(
      "\\data D Nat | D (suc n) => c\n" +
      "\\function f => D (suc zero)\n" +
      "\\function test (x : f) : Nat <= \\elim x\n" +
          " | c => 0"
    );
  }

  @Test
  public void elimFail() {
      typeCheckClass("\\function\n" +
                     "test (x y : Nat) : y = 0 <= \\elim x, y\n" +
                     "| _, zero => path (\\lam _ => zero)\n" +
                     "| zero, suc y' => test x y'\n" +
                     "| suc x', suc y' => test x y'\n" +
                     "\n" +
                     "\\function\n" +
                     "zero-is-one : 1 = 0 => test 0 1", 3);
  }

  @Test
  public void testSmthing() {
    typeCheckClass(
        "\\data Geq (x y : Nat)\n" +
        "  | Geq m zero => EqBase \n" +
        "  | Geq (suc n) (suc m) => EqSuc (p : Geq n m)\n" +
        "\n" +
        "\\function f (x y : Nat) (p : Geq x y) : Nat <=\n" +
        "  \\case x, y, p\n" +
        "    | m, zero, EqBase <= zero \n" +
        "    | zero, suc _, _!\n" +
        "    | suc _, suc _, EqSuc q <= suc zero", 3);
  }

  @Test
  public void testElimOrderError() {
    typeCheckClass("\\data \\infix 4\n" +
                   "(=<) (n m : Nat)\n" +
                   "  | (=<) zero m => le_z\n" +
                   "  | (=<) (suc n) (suc m) => le_ss (n =< m)\n" +

                   "\\function\n" +
                   "leq-trans {n m k : Nat} (nm : n =< m) (mk : m =< k) : n =< k <= \\elim n, nm, m\n" +
                   "  | zero, le_z, _ => {?}\n" +
                   "  | suc n', le_ss nm', suc m' => {?}", 1);
  }

  @Test
  public void arrowTest() {
    typeCheckDef(
        "\\function (+) (x y : Nat) : Nat => \\elim x" +
        "  | zero => y\n" +
        "  | suc x => suc (x + y)", 1);
  }

  @Test
  public void testAnyNoElimError() {
    typeCheckClass(
        "\\data D Nat | D zero => d0\n" +
            "\\function test (x : Nat) (d : D x) : Nat <= \\elim d\n" +
            " | _! => 0", 1
    );
  }

  @Test
  public void testElimTranslationSubst() {
    FunctionDefinition def = (FunctionDefinition) typeCheckDef(
      "\\function test (n m : Nat) : Nat <= \\elim m\n" +
      " | zero => n\n" +
      " | _ => n\n"
    );
    assertEquals(def.getElimTree(), top(def.getParameters(), branch(def.getParameters().getNext(), tail(), clause(Prelude.ZERO, EmptyDependentLink.getInstance(), Reference(def.getParameters())), clause(Reference(def.getParameters())))));
  }

  @Test
  public void testElimOnIntervalError() {
    typeCheckDef(
        "\\function test (i : I) : Nat <= \\elim i\n" +
        " | left => 0\n" +
        " | right => 1\n"  +
        " | _ => 0\n"
    , 2);
  }

  @Test
  public void emptyAfterAFew() {
    typeCheckClass(
        "\\data D Nat | D zero => d\n" +
        "\\function test (x : Nat) (y : \\Pi(z : Nat) -> x = z) (a : D (suc x)) : Nat <= \\elim x\n");
  }

  @Test
  public void testElimEmpty1() {
    typeCheckClass(
        "\\data D Nat | D zero => d1 | D (suc zero) => d2 \n" +
        "\\data E (n : Nat) | e (D n)\n" +
        "\\function test (n : Nat) (e : E n) : Nat <= \\elim n, e\n" +
            " | zero, _ => 0\n" +
            " | suc zero, _ => 1\n" +
            " | suc (suc _), e (_!)"
    );
  }

  @Test
  public void testMultiArg() {
    typeCheckClass(
      "\\data D (A B : \\Type0) | c A B\n" +
      "\\function test (f : Nat -> Nat) (d : D Nat (Nat -> Nat)) : Nat <= \\elim d\n" +
          " | c x y => f x"
    );
  }

  @Test
  public void testEmptyLet() {
    typeCheckClass(
        "\\data D\n" +
        "\\function test (d : D) : 0 = 1 <= \\let x (d : D) : 0 = 1 <= \\elim d \\in x d"
    );
  }
}

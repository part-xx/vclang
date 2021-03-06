package com.jetbrains.jetpad.vclang.term.expr;

import com.jetbrains.jetpad.vclang.term.Prelude;
import com.jetbrains.jetpad.vclang.core.context.param.DependentLink;
import com.jetbrains.jetpad.vclang.core.definition.*;
import com.jetbrains.jetpad.vclang.core.expr.Expression;
import com.jetbrains.jetpad.vclang.core.sort.Level;
import com.jetbrains.jetpad.vclang.core.sort.Sort;
import com.jetbrains.jetpad.vclang.core.sort.LevelArguments;
import com.jetbrains.jetpad.vclang.core.expr.type.Type;
import com.jetbrains.jetpad.vclang.core.expr.type.TypeMax;
import com.jetbrains.jetpad.vclang.core.expr.visitor.NormalizeVisitor;
import com.jetbrains.jetpad.vclang.core.pattern.elimtree.LeafElimTreeNode;
import com.jetbrains.jetpad.vclang.typechecking.TypeCheckingTestCase;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static com.jetbrains.jetpad.vclang.core.expr.ExpressionFactory.*;
import static org.junit.Assert.assertEquals;

public class GetTypeTest extends TypeCheckingTestCase {
  private static void testType(Expression expected, TypeCheckClassResult result) {
    assertEquals(expected, ((FunctionDefinition) result.getDefinition("test")).getResultType().toExpression());
    assertEquals(expected, ((LeafElimTreeNode) ((FunctionDefinition) result.getDefinition("test")).getElimTree()).getExpression().getType().toExpression());
  }

  @Test
  public void constructorTest() {
    TypeCheckClassResult result = typeCheckClass("\\data List (A : \\1-Type0) | nil | cons A (List A) \\function test => cons 0 nil");
    testType(DataCall((DataDefinition) result.getDefinition("List"), new LevelArguments(), Nat()), result);
  }

  @Test
  public void nilConstructorTest() {
    TypeCheckClassResult result = typeCheckClass("\\data List (A : \\1-Type0) | nil | cons A (List A) \\function test => (List Nat).nil");
    testType(DataCall((DataDefinition) result.getDefinition("List"), new LevelArguments(), Nat()), result);
  }

  @Test
  public void classExtTest() {
    TypeCheckClassResult result = typeCheckClass("\\class Test { \\field A : \\Type0 \\field a : A } \\function test => Test { A => Nat }");
    assertEquals(Universe(1), result.getDefinition("Test").getTypeWithParams(new ArrayList<DependentLink>(), new LevelArguments()).toExpression());
    assertEquals(Universe(Sort.SET0), result.getDefinition("test").getTypeWithParams(new ArrayList<DependentLink>(), new LevelArguments()).toExpression());
    testType(Universe(Sort.SET0), result);
  }

  @Test
  public void lambdaTest() {
    TypeCheckClassResult result = typeCheckClass("\\function test => \\lam (f : Nat -> Nat) => f 0");
    testType(Pi(Pi(Nat(), Nat()), Nat()), result);
  }

  @Test
  public void lambdaTest2() {
    TypeCheckClassResult result = typeCheckClass("\\function test => \\lam (A : \\Type0) (x : A) => x");
    DependentLink A = param("A", Universe(0));
    Expression expectedType = Pi(params(A, param("x", Reference(A))), Reference(A));
    testType(expectedType, result);
  }

  @Test
  public void fieldAccTest() {
    TypeCheckClassResult result = typeCheckClass("\\class C { \\field x : Nat \\function f (p : 0 = x) => p } \\function test (p : Nat -> C) => (p 0).f");
    DependentLink p = param("p", Pi(Nat(), ClassCall((ClassDefinition) result.getDefinition("C"), new LevelArguments())));
    Expression type = FunCall(Prelude.PATH_INFIX, new Level(0), new Level(1),
        Nat(),
        Zero(),
        FieldCall((ClassField) result.getDefinition("C.x"), Apps(Reference(p), Zero())));
    List<DependentLink> testParams = new ArrayList<>();
    TypeMax testType = result.getDefinition("test").getTypeWithParams(testParams, new LevelArguments());
    assertEquals(Pi(p, Pi(type, type)).normalize(NormalizeVisitor.Mode.NF), testType.fromPiParameters(testParams).normalize(NormalizeVisitor.Mode.NF));
  }

  @Test
  public void tupleTest() {
    TypeCheckClassResult result = typeCheckClass("\\function test : \\Sigma (x y : Nat) (x = y) => (0, 0, path (\\lam _ => 0))");
    DependentLink xy = param(true, vars("x", "y"), Nat());
    testType(Sigma(params(xy, param(FunCall(Prelude.PATH_INFIX, new LevelArguments(Arrays.asList(new Level(0), new Level(1))), Nat(), Reference(xy), Reference(xy.getNext()))))), result);
  }

  @Test
  public void letTest() {
    Definition def = typeCheckDef("\\function test => \\lam (F : Nat -> \\Type0) (f : \\Pi (x : Nat) -> F x) => \\let | x => 0 \\in f x");
    DependentLink F = param("F", Pi(Nat(), Universe(0)));
    DependentLink x = param("x", Nat());
    DependentLink f = param("f", Pi(x, Apps(Reference(F), Reference(x))));
    assertEquals(Pi(params(F, f), Apps(Reference(F), Zero())), ((Expression) ((LeafElimTreeNode) ((FunctionDefinition) def).getElimTree()).getExpression().getType()).normalize(NormalizeVisitor.Mode.NF));
  }

  @Test
  public void patternConstructor1() {
    TypeCheckClassResult result = typeCheckClass(
        "\\data C (n : Nat) | C (zero) => c1 | C (suc n) => c2 Nat");
    DataDefinition data = (DataDefinition) result.getDefinition("C");
    List<DependentLink> c1Params = new ArrayList<>();
    Type c1Type = data.getConstructor("c1").getTypeWithParams(c1Params, new LevelArguments());
    assertEquals(DataCall(data, new LevelArguments(), Zero()), c1Type);
    List<DependentLink> c2Params = new ArrayList<>();
    Type c2Type = data.getConstructor("c2").getTypeWithParams(c2Params, new LevelArguments());
    DependentLink params = data.getConstructor("c2").getDataTypeParameters();
    assertEquals(
        Pi(params, Pi(param(Nat()), DataCall(data, new LevelArguments(), Suc(Reference(params))))),
        c2Type.fromPiParameters(c2Params)
    );
  }

  @Test
  public void patternConstructor2() {
    TypeCheckClassResult result = typeCheckClass(
        "\\data Vec \\Set0 Nat | Vec A zero => Nil | Vec A (suc n) => Cons A (Vec A n)" +
        "\\data D (n : Nat) (Vec Nat n) | D zero _ => dzero | D (suc n) _ => done");
    DataDefinition vec = (DataDefinition) result.getDefinition("Vec");
    DataDefinition d = (DataDefinition) result.getDefinition("D");
    List<DependentLink> dzeroParams = new ArrayList<>();
    Type dzeroType = d.getConstructor("dzero").getTypeWithParams(dzeroParams, new LevelArguments());
    assertEquals(
        Pi(d.getConstructor("dzero").getDataTypeParameters(), DataCall(d, new LevelArguments(), Zero(), Reference(d.getConstructor("dzero").getDataTypeParameters()))),
        dzeroType.fromPiParameters(dzeroParams)
    );
    List<DependentLink> doneAllParams = new ArrayList<>();
    Type doneType = d.getConstructor("done").getTypeWithParams(doneAllParams, new LevelArguments());
    DependentLink doneParams = d.getConstructor("done").getDataTypeParameters();
    assertEquals(
        Pi(d.getConstructor("done").getDataTypeParameters(), DataCall(d, new LevelArguments(), Suc(Reference(doneParams)), Reference(doneParams.getNext()))),
        doneType.fromPiParameters(doneAllParams)
    );
    List<DependentLink> consAllParams = new ArrayList<>();
    Type consType = vec.getConstructor("Cons").getTypeWithParams(consAllParams, new LevelArguments());
    DependentLink consParams = vec.getConstructor("Cons").getDataTypeParameters();
    assertEquals(
        Pi(consParams, Pi(Reference(consParams), Pi(DataCall(vec, new LevelArguments(), Reference(consParams), Reference(consParams.getNext())), DataCall(vec, new LevelArguments(), Reference(consParams), Suc(Reference(consParams.getNext())))))),
        consType.fromPiParameters(consAllParams)
    );
  }

  @Test
  public void patternConstructor3() {
    TypeCheckClassResult result = typeCheckClass(
        "\\data D | d \\Type0\n" +
        "\\data C D | C (d A) => c A");
    DataDefinition d = (DataDefinition) result.getDefinition("D");
    DataDefinition c = (DataDefinition) result.getDefinition("C");
    DependentLink A = c.getConstructor("c").getDataTypeParameters();
    List<DependentLink> cParams = new ArrayList<>();
    Type cType = c.getConstructor("c").getTypeWithParams(cParams, new LevelArguments());
    assertEquals(
        Pi(c.getConstructor("c").getDataTypeParameters(), Pi(Reference(A), DataCall(c, new LevelArguments(), ConCall(d.getConstructor("d"), new LevelArguments(), Collections.<Expression>emptyList(), Reference(A))))),
        cType.fromPiParameters(cParams)
    );
  }

  @Test
  public void patternConstructorDep() {
    TypeCheckClassResult result = typeCheckClass(
        "\\data Box (n : Nat) | box\n" +
        "\\data D (n : Nat) (Box n) | D (zero) _ => d");
    DataDefinition d = (DataDefinition) result.getDefinition("D");
    List<DependentLink> dParams = new ArrayList<>();
    Type dType = d.getConstructor("d").getTypeWithParams(dParams, new LevelArguments());
    assertEquals(
        Pi(d.getConstructor("d").getDataTypeParameters(), DataCall(d, new LevelArguments(), Zero(), Reference(d.getConstructor("d").getDataTypeParameters()))),
        dType.fromPiParameters(dParams)
    );
  }
}

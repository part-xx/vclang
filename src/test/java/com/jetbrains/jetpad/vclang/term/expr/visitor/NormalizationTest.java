package com.jetbrains.jetpad.vclang.term.expr.visitor;

import com.jetbrains.jetpad.vclang.core.context.binding.Binding;
import com.jetbrains.jetpad.vclang.core.context.binding.LevelBinding;
import com.jetbrains.jetpad.vclang.core.context.binding.TypedBinding;
import com.jetbrains.jetpad.vclang.core.context.param.DependentLink;
import com.jetbrains.jetpad.vclang.core.context.param.EmptyDependentLink;
import com.jetbrains.jetpad.vclang.core.definition.Constructor;
import com.jetbrains.jetpad.vclang.core.definition.DataDefinition;
import com.jetbrains.jetpad.vclang.core.definition.Definition;
import com.jetbrains.jetpad.vclang.core.definition.FunctionDefinition;
import com.jetbrains.jetpad.vclang.core.expr.Expression;
import com.jetbrains.jetpad.vclang.core.expr.LetClause;
import com.jetbrains.jetpad.vclang.core.expr.visitor.NormalizeVisitor;
import com.jetbrains.jetpad.vclang.core.pattern.elimtree.ElimTreeNode;
import com.jetbrains.jetpad.vclang.core.pattern.elimtree.LeafElimTreeNode;
import com.jetbrains.jetpad.vclang.core.sort.Level;
import com.jetbrains.jetpad.vclang.core.sort.LevelArguments;
import com.jetbrains.jetpad.vclang.frontend.Concrete;
import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.Prelude;
import com.jetbrains.jetpad.vclang.typechecking.TypeCheckingTestCase;
import com.jetbrains.jetpad.vclang.typechecking.visitor.CheckTypeVisitor;
import org.junit.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.jetbrains.jetpad.vclang.core.expr.ExpressionFactory.*;
import static com.jetbrains.jetpad.vclang.frontend.ConcreteExpressionFactory.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class NormalizationTest extends TypeCheckingTestCase {
  // \function (+) (x y : Nat) : Nat <= elim x | zero => y | suc x' => suc (x' + y)
  private final FunctionDefinition plus;
  // \function (*) (x y : Nat) : Nat <= elim x | zero => zero | suc x' => y + x' * y
  private final FunctionDefinition mul;
  // \function fac (x : Nat) : Nat <= elim x | zero => suc zero | suc x' => suc x' * fac x'
  private final FunctionDefinition fac;
  // \function nelim (z : Nat) (s : Nat -> Nat -> Nat) (x : Nat) : Nat <= elim x | zero => z | suc x' => s x' (nelim z s x')
  private final FunctionDefinition nelim;

  private DataDefinition bdList;
  private Constructor bdNil;
  private Constructor bdCons;
  private Constructor bdSnoc;

  public NormalizationTest() throws IOException {
    DependentLink xPlus = param("x", Nat());
    DependentLink yPlus = param("y", Nat());
    plus = new FunctionDefinition(null, params(xPlus, yPlus), Nat(), null);

    DependentLink xPlusMinusOne = param("x'", Nat());
    ElimTreeNode plusElimTree = top(xPlus, branch(xPlus, tail(yPlus),
        clause(Prelude.ZERO, EmptyDependentLink.getInstance(), Reference(yPlus)),
        clause(Prelude.SUC, xPlusMinusOne, Suc(FunCall(plus, new LevelArguments(), Reference(xPlusMinusOne), Reference(yPlus))))));
    plus.setElimTree(plusElimTree);
    plus.hasErrors(Definition.TypeCheckingStatus.NO_ERRORS);

    DependentLink xMul = param("x", Nat());
    DependentLink yMul = param("y", Nat());
    mul = new FunctionDefinition(null, params(xMul, yMul), Nat(), null);
    DependentLink xMulMinusOne = param("x'", Nat());
    ElimTreeNode mulElimTree = top(xMul, branch(xMul, tail(yMul),
        clause(Prelude.ZERO, EmptyDependentLink.getInstance(), Zero()),
        clause(Prelude.SUC, xMulMinusOne, FunCall(plus, new LevelArguments(), Reference(yMul), FunCall(mul, new LevelArguments(), Reference(xMulMinusOne), Reference(yMul))))
    ));
    mul.setElimTree(mulElimTree);
    mul.hasErrors(Definition.TypeCheckingStatus.NO_ERRORS);

    DependentLink xFac = param("x", Nat());
    fac = new FunctionDefinition(null, xFac, Nat(), null);
    DependentLink xFacMinusOne = param("x'", Nat());
    ElimTreeNode facElimTree = top(xFac, branch(xFac, tail(),
        clause(Prelude.ZERO, EmptyDependentLink.getInstance(), Suc(Zero())),
        clause(Prelude.SUC, xFacMinusOne, FunCall(mul, new LevelArguments(), Suc(Reference(xFacMinusOne)), FunCall(fac, new LevelArguments(), Reference(xFacMinusOne))))
    ));
    fac.setElimTree(facElimTree);
    fac.hasErrors(Definition.TypeCheckingStatus.NO_ERRORS);

    DependentLink zNElim = param("z", Nat());
    DependentLink sNElim = param("s", Pi(param(Nat()), Pi(param(Nat()), Nat())));
    DependentLink xNElim = param("x", Nat());
    nelim = new FunctionDefinition(null, params(zNElim, sNElim, xNElim), Nat(), null);
    DependentLink xNElimMinusOne = param("x'", Nat());
    ElimTreeNode nelimElimTree = top(zNElim, branch(xNElim, tail(),
        clause(Prelude.ZERO, EmptyDependentLink.getInstance(), Reference(zNElim)),
        clause(Prelude.SUC, xNElimMinusOne, Apps(Reference(sNElim), Reference(xNElimMinusOne), FunCall(nelim, new LevelArguments(), Reference(zNElim), Reference(sNElim), Reference(xNElimMinusOne))))
    ));
    nelim.setElimTree(nelimElimTree);
    nelim.hasErrors(Definition.TypeCheckingStatus.NO_ERRORS);
  }

  private void initializeBDList() {
    TypeCheckingTestCase.TypeCheckClassResult result = typeCheckClass(
        "\\data BD-list (A : \\Set0) | nil | cons A (BD-list A) | snoc (BD-list A) A\n" +
        "  \\with | snoc (cons x xs) x => cons x (snoc xs x) | snoc nil x => cons x nil\n"
    );
    bdList = (DataDefinition) result.getDefinition("BD-list");
    bdNil = bdList.getConstructor("nil");
    bdCons = bdList.getConstructor("cons");
    bdSnoc = bdList.getConstructor("snoc");
  }

  @Test
  public void normalizeLamId() {
    // normalize( (\x.x) (suc zero) ) = suc zero
    DependentLink x = param("x", Nat());
    Expression expr = Apps(Lam(x, Reference(x)), Suc(Zero()));
    assertEquals(Suc(Zero()), expr.normalize(NormalizeVisitor.Mode.NF));
  }

  @Test
  public void normalizeLamK() {
    // normalize( (\x y. x) (suc zero) ) = \z. suc zero
    DependentLink x = param("x", Nat());
    DependentLink y = param("y", Nat());
    DependentLink z = param("z", Nat());
    Expression expr = Apps(Lam(x, Lam(y, Reference(x))), Suc(Zero()));
    assertEquals(Lam(z, Suc(Zero())), expr.normalize(NormalizeVisitor.Mode.NF));
  }

  @Test
  public void normalizeLamKstar() {
    // normalize( (\x y. y) (suc zero) ) = \z. z
    DependentLink x = param("x", Nat());
    DependentLink y = param("y", Nat());
    DependentLink z = param("z", Nat());
    Expression expr = Apps(Lam(x, Lam(y, Reference(y))), Suc(Zero()));
    assertEquals(Lam(z, Reference(z)), expr.normalize(NormalizeVisitor.Mode.NF));
  }

  @Test
  public void normalizeLamKOpen() {
    // normalize( (\x y. x) (suc (var(0))) ) = \z. suc (var(0))
    DependentLink var0 = param("var0", Universe(0));
    DependentLink x = param("x", Nat());
    DependentLink y = param("y", Nat());
    DependentLink z = param("z", Nat());
    Expression expr = Apps(Lam(params(x, y), Reference(x)), Suc(Reference(var0)));
    assertEquals(Lam(z, Suc(Reference(var0))), expr.normalize(NormalizeVisitor.Mode.NF));
  }

  @Test
  public void normalizeNelimZero() {
    // normalize( N-elim (suc zero) (\x. suc x) 0 ) = suc zero
    DependentLink x = param("x", Nat());
    Expression expr = FunCall(nelim, new LevelArguments(), Suc(Zero()), Lam(x, Suc(Reference(x))), Zero());
    assertEquals(Suc(Zero()), expr.normalize(NormalizeVisitor.Mode.NF));
  }

  @Test
  public void normalizeNelimOne() {
    // normalize( N-elim (suc zero) (\x y. (var(0)) y) (suc zero) ) = var(0) (suc zero)
    DependentLink var0 = param("var0", Pi(Nat(), Nat()));
    DependentLink x = param("x", Nat());
    DependentLink y = param("y", Nat());
    Expression expr = FunCall(nelim, new LevelArguments(), Suc(Zero()), Lam(x, Lam(y, Apps(Reference(var0), Reference(y)))), Suc(Zero()));
    assertEquals(Apps(Reference(var0), Suc(Zero())), expr.normalize(NormalizeVisitor.Mode.NF));
  }

  @Test
  public void normalizeNelimArg() {
    // normalize( N-elim (suc zero) (var(0)) ((\x. x) zero) ) = suc zero
    DependentLink var0 = param("var0", Universe(0));
    DependentLink x = param("x", Nat());
    Expression arg = Apps(Lam(x, Reference(x)), Zero());
    Expression expr = FunCall(nelim, new LevelArguments(), Suc(Zero()), Reference(var0), arg);
    Expression result = expr.normalize(NormalizeVisitor.Mode.NF);
    assertEquals(Suc(Zero()), result);
  }

  @Test
  public void normalizePlus0a3() {
    // normalize (plus 0 3) = 3
    Expression expr = FunCall(plus, new LevelArguments(), Zero(), Suc(Suc(Suc(Zero()))));
    assertEquals(Suc(Suc(Suc(Zero()))), expr.normalize(NormalizeVisitor.Mode.NF));
  }

  @Test
  public void normalizePlus3a0() {
    // normalize (plus 3 0) = 3
    Expression expr = FunCall(plus, new LevelArguments(), Suc(Suc(Suc(Zero()))), Zero());
    assertEquals(Suc(Suc(Suc(Zero()))), expr.normalize(NormalizeVisitor.Mode.NF));
  }

  @Test
  public void normalizePlus3a3() {
    // normalize (plus 3 3) = 6
    Expression expr = FunCall(plus, new LevelArguments(), Suc(Suc(Suc(Zero()))), Suc(Suc(Suc(Zero()))));
    assertEquals(Suc(Suc(Suc(Suc(Suc(Suc(Zero())))))), expr.normalize(NormalizeVisitor.Mode.NF));
  }

  @Test
  public void normalizeMul3a0() {
    // normalize (mul 3 0) = 0
    Expression expr = FunCall(mul, new LevelArguments(), Suc(Suc(Suc(Zero()))), Zero());
    assertEquals(Zero(), expr.normalize(NormalizeVisitor.Mode.NF));
  }

  @Test
  public void normalizeMul0a3() {
    // normalize (mul 0 3) = 0
    Expression expr = FunCall(mul, new LevelArguments(), Zero(), Suc(Suc(Suc(Zero()))));
    assertEquals(Zero(), expr.normalize(NormalizeVisitor.Mode.NF));
  }

  @Test
  public void normalizeMul3a3() {
    // normalize (mul 3 3) = 9
    Expression expr = FunCall(mul, new LevelArguments(), Suc(Suc(Suc(Zero()))), Suc(Suc(Suc(Zero()))));
    assertEquals(Suc(Suc(Suc(Suc(Suc(Suc(Suc(Suc(Suc(Zero()))))))))), expr.normalize(NormalizeVisitor.Mode.NF));
  }

  @Test
  public void normalizeFac3() {
    // normalize (fac 3) = 6
    Expression expr = FunCall(fac, new LevelArguments(), Suc(Suc(Suc(Zero()))));
    assertEquals(Suc(Suc(Suc(Suc(Suc(Suc(Zero())))))), expr.normalize(NormalizeVisitor.Mode.NF));
  }

  @Test
  public void normalizeLet1() {
    // normalize (\let | x => zero \in \let | y = suc \in y x) = 1
    CheckTypeVisitor.Result result = typeCheckExpr(cLet(clets(clet("x", cZero())), cLet(clets(clet("y", cSuc())), cApps(cVar("y"), cVar("x")))), null);
    assertEquals(Suc(Zero()), result.expression.normalize(NormalizeVisitor.Mode.NF));
  }

  @Test
  public void normalizeLet2() {
    // normalize (\let | x => suc \in \let | y = zero \in x y) = 1
    CheckTypeVisitor.Result result = typeCheckExpr(cLet(clets(clet("x", cSuc())), cLet(clets(clet("y", cZero())), cApps(cVar("x"), cVar("y")))), null);
    assertEquals(Suc(Zero()), result.expression.normalize(NormalizeVisitor.Mode.NF));
  }

  @Test
  public void normalizeLetNo() {
    // normalize (\let | x (y z : N) => zero \in x zero) = \lam (z : N) => zero
    CheckTypeVisitor.Result result = typeCheckExpr("\\let x (y z : Nat) => 0 \\in x 0", null);
    DependentLink x = param("x", Nat());
    assertEquals(Lam(x, Zero()), result.expression.normalize(NormalizeVisitor.Mode.NF));
  }

  @Test
  public void normalizeLetElimStuck() {
    // normalize (\let | x (y : N) : N <= \elim y | zero => zero | suc _ => zero \in x <1>) = the same
    List<Binding> context = new ArrayList<>();
    context.add(new TypedBinding("n", Nat()));
    CheckTypeVisitor.Result result = typeCheckExpr(context, "\\let x (y : Nat) : Nat <= \\elim y | zero => zero | suc _ => zero \\in x n", null);
    assertEquals(result.expression, result.expression.normalize(NormalizeVisitor.Mode.NF));
  }

  @Test
  public void normalizeLetElimNoStuck() {
    // normalize (\let | x (y : N) : \Type2 <= \elim y | \Type0 => \Type1 | succ _ => \Type1 \in x zero) = \Type0
    Concrete.Expression elimTree = cElim(Collections.<Concrete.Expression>singletonList(cVar("y")),
        cClause(cPatterns(cConPattern(Prelude.ZERO.getName())), Abstract.Definition.Arrow.RIGHT, cUniverse(0)),
        cClause(cPatterns(cConPattern(Prelude.SUC.getName(), cPatternArg(cNamePattern(null), true, false))), Abstract.Definition.Arrow.RIGHT, cUniverse(1))
    );
    CheckTypeVisitor.Result result = typeCheckExpr(cLet(clets(clet("x", cargs(cTele(cvars("y"), cNat())), cUniverse(2), Abstract.Definition.Arrow.LEFT, elimTree)), cApps(cVar("x"), cZero())), null);
    assertEquals(Universe(0), result.expression.normalize(NormalizeVisitor.Mode.NF));
  }

  @Test
  public void normalizeElimAnyConstructor() {
    DependentLink var0 = param("var0", Universe(0));
    TypeCheckingTestCase.TypeCheckClassResult result = typeCheckClass(
        "\\data D | d Nat\n" +
        "\\function test (x : D) : Nat <= \\elim x | _! => 0");
    FunctionDefinition test = (FunctionDefinition) result.getDefinition("test");
    assertEquals(FunCall(test, new LevelArguments(), Reference(var0)), FunCall(test, new LevelArguments(), Reference(var0)).normalize(NormalizeVisitor.Mode.NF));
  }

  @Test
  public void letNormalizationContext() {
    LetClause let = let("x", EmptyDependentLink.getInstance(), Nat(), top(EmptyDependentLink.getInstance(), leaf(Abstract.Definition.Arrow.RIGHT, Zero())));
    Let(lets(let), Reference(let)).normalize(NormalizeVisitor.Mode.NF);
  }

  @Test
  public void testConditionNormalization() {
    typeCheckClass(
        "\\data Z | pos Nat | neg Nat \\with | pos zero => neg 0\n" +
        "\\function only-one-zero : pos 0 = neg 0 => path (\\lam _ => pos 0)"
    );
  }

  @Test
  public void testConCallNormFull() {
    initializeBDList();
    Expression expr1 = ConCall(bdSnoc, new LevelArguments(), Collections.<Expression>singletonList(Nat()), ConCall(bdNil, new LevelArguments(), Collections.<Expression>singletonList(Nat())), Zero());
    assertEquals(ConCall(bdCons, new LevelArguments(), Collections.<Expression>singletonList(Nat()), Zero(), ConCall(bdNil, new LevelArguments(), Collections.<Expression>singletonList(Nat()))), expr1.normalize(NormalizeVisitor.Mode.NF));
  }

  @Test
  public void testIsoleft() {
    DependentLink A = param("A", Universe(new Level(0), new Level(0)));
    DependentLink B = param("B", Universe(new Level(0), new Level(0)));
    DependentLink f = param("f", Pi(param(Reference(A)), Reference(B)));
    DependentLink g = param("g", Pi(param(Reference(B)), Reference(A)));
    DependentLink a = param("a", Reference(A));
    DependentLink b = param("b", Reference(B));
    Expression linvType = FunCall(Prelude.PATH_INFIX, new Level(0), new Level(0),
        Reference(A),
        Apps(Reference(g), Apps(Reference(f), Reference(a))),
        Reference(a));
    DependentLink linv = param("linv", Pi(a, linvType));
    Expression rinvType = FunCall(Prelude.PATH_INFIX, new Level(0), new Level(0),
        Reference(B),
        Apps(Reference(f), Apps(Reference(g), Reference(b))),
        Reference(b));
    DependentLink rinv = param("rinv", Pi(b, rinvType));
    Expression iso_expr = FunCall(Prelude.ISO, new Level(0), new Level(0),
        Reference(A), Reference(B),
        Reference(f), Reference(g),
        Reference(linv), Reference(rinv),
        Left());
    assertEquals(Reference(A), iso_expr.normalize(NormalizeVisitor.Mode.NF));
  }

  @Test
  public void testIsoRight() {
    LevelBinding lp = Prelude.PATH.getPolyParams().get(0);
    LevelBinding lh = Prelude.PATH.getPolyParams().get(1);
    DependentLink A = param("A", Universe(new Level(lp), new Level(lh)));
    DependentLink B = param("B", Universe(new Level(lp), new Level(lh)));
    DependentLink f = param("f", Pi(param(Reference(A)), Reference(B)));
    DependentLink g = param("g", Pi(param(Reference(B)), Reference(A)));
    DependentLink a = param("a", Reference(A));
    DependentLink b = param("b", Reference(B));
    Expression linvType = FunCall(Prelude.PATH_INFIX, new Level(lp), new Level(lh),
        Reference(A),
        Apps(Reference(g), Apps(Reference(f), Reference(a))),
        Reference(a));
    DependentLink linv = param("linv", Pi(a, linvType));
    Expression rinvType = FunCall(Prelude.PATH_INFIX, new Level(lp), new Level(lh),
        Reference(B),
        Apps(Reference(f), Apps(Reference(g), Reference(b))),
        Reference(b));
    DependentLink rinv = param("rinv", Pi(b, rinvType));
    Expression iso_expr = FunCall(Prelude.ISO, new Level(lp), new Level(lh),
        Reference(A), Reference(B),
        Reference(f), Reference(g),
        Reference(linv), Reference(rinv),
        Right());
    assertEquals(Reference(B), iso_expr.normalize(NormalizeVisitor.Mode.NF));
  }

  @Test
  public void testCoeIso() {
    DependentLink A = param("A", Universe(new Level(0), new Level(0)));
    DependentLink B = param("B", Universe(new Level(0), new Level(0)));
    DependentLink f = param("f", Pi(param(Reference(A)), Reference(B)));
    DependentLink g = param("g", Pi(param(Reference(B)), Reference(A)));
    DependentLink a = param("a", Reference(A));
    DependentLink b = param("b", Reference(B));
    DependentLink k = param("k", Interval());
    Expression linvType = FunCall(Prelude.PATH_INFIX, new Level(0), new Level(0),
        Reference(A),
        Apps(Reference(g), Apps(Reference(f), Reference(a))),
        Reference(a));
    DependentLink linv = param("linv", Pi(a, linvType));
    Expression rinvType = FunCall(Prelude.PATH_INFIX, new Level(0), new Level(0),
        Reference(B),
        Apps(Reference(f), Apps(Reference(g), Reference(b))),
        Reference(b));
    DependentLink rinv = param("rinv", Pi(b, rinvType));
    DependentLink aleft = param("aleft", Reference(A));
    Expression iso_expr = FunCall(Prelude.ISO, new Level(0), new Level(0),
        Reference(A), Reference(B),
        Reference(f), Reference(g),
        Reference(linv), Reference(rinv),
        Reference(k));
    Expression expr = FunCall(Prelude.COERCE, new Level(0), new Level(0),
        Lam(k, iso_expr),
        Reference(aleft),
        Right());
    assertEquals(Apps(Reference(f), Reference(aleft)), expr.normalize(NormalizeVisitor.Mode.NF));
  }

  @Test
  public void testCoeIsoFreeVar() {
    DependentLink k = param("k", Interval());
    DependentLink i = param("i", Interval());
    Expression A = DataCall(Prelude.PATH, new Level(0), new Level(0), Lam(i, Interval()), Reference(k), Reference(k));
    DependentLink B = param("B", Universe(new Level(0), new Level(0)));
    DependentLink f = param("f", Pi(param(A), Reference(B)));
    DependentLink g = param("g", Pi(param(Reference(B)), A));
    DependentLink a = param("a", A);
    DependentLink b = param("b", Reference(B));
    Expression linvType = FunCall(Prelude.PATH_INFIX, new Level(0), new Level(0),
        A,
        Apps(Reference(g), Apps(Reference(f), Reference(a))),
        Reference(a));
    DependentLink linv = param("linv", Pi(a, linvType));
    Expression rinvType = FunCall(Prelude.PATH_INFIX, new Level(0), new Level(0),
        Reference(B),
        Apps(Reference(f), Apps(Reference(g), Reference(b))),
        Reference(b));
    DependentLink rinv = param("rinv", Pi(b, rinvType));
    DependentLink aleft = param("aleft", A.subst(k, Right()));
    Expression expr = FunCall(Prelude.COERCE, new Level(0), new Level(0),
        Lam(k, FunCall(Prelude.ISO, new Level(0), new Level(0),
            DataCall(Prelude.PATH, new Level(0), new Level(0),
                Lam(i, Interval()),
                Reference(k),
                Reference(k)),
            Reference(B),
            Reference(f),
            Reference(g),
            Reference(linv),
            Reference(rinv),
            Reference(k))),
        Reference(aleft),
        Right());
    assertEquals(expr, expr.normalize(NormalizeVisitor.Mode.NF));
  }

  @Test
  public void testAppProj() {
    DependentLink x = param("x", Nat());
    Expression expr = Apps(Proj(Tuple(Sigma(param(Pi(param(Nat()), Nat()))), Lam(x, Reference(x))), 0), Zero());
    assertEquals(Zero(), expr.normalize(NormalizeVisitor.Mode.NF));
  }

  @Test
  public void testConCallEta() {
    TypeCheckClassResult result = typeCheckClass(
        "\\function ($) {X Y : \\Type0} (f : X -> Y) (x : X) => f x\n" +
        "\\data Fin (n : Nat)\n" +
        "  | Fin (suc n) => fzero\n" +
        "  | Fin (suc n) => fsuc (Fin n)\n" +
        "\\function f (n : Nat) (x : Fin n) => fsuc $ x");
    FunctionDefinition f = (FunctionDefinition) result.getDefinition("f");
    Expression term = ((LeafElimTreeNode) f.getElimTree()).getExpression().normalize(NormalizeVisitor.Mode.NF);
    assertNotNull(term.toConCall());
    assertEquals(result.getDefinition("fsuc"), term.toConCall().getDefinition());
    assertEquals(1, term.toConCall().getDefCallArguments().size());
    assertNotNull(term.toConCall().getDefCallArguments().get(0).toReference());
    assertEquals(f.getParameters().getNext(), term.toConCall().getDefCallArguments().get(0).toReference().getBinding());
  }
}

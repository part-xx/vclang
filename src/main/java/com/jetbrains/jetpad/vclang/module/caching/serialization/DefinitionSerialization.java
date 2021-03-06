package com.jetbrains.jetpad.vclang.module.caching.serialization;

import com.jetbrains.jetpad.vclang.core.context.binding.*;
import com.jetbrains.jetpad.vclang.core.context.param.DependentLink;
import com.jetbrains.jetpad.vclang.core.context.param.TypedDependentLink;
import com.jetbrains.jetpad.vclang.core.definition.ClassField;
import com.jetbrains.jetpad.vclang.core.expr.*;
import com.jetbrains.jetpad.vclang.core.expr.type.PiUniverseType;
import com.jetbrains.jetpad.vclang.core.expr.type.TypeMax;
import com.jetbrains.jetpad.vclang.core.expr.visitor.ExpressionVisitor;
import com.jetbrains.jetpad.vclang.core.internal.FieldSet;
import com.jetbrains.jetpad.vclang.core.pattern.*;
import com.jetbrains.jetpad.vclang.core.pattern.elimtree.*;
import com.jetbrains.jetpad.vclang.core.pattern.elimtree.visitor.ElimTreeNodeVisitor;
import com.jetbrains.jetpad.vclang.core.sort.*;
import com.jetbrains.jetpad.vclang.term.Abstract;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class DefinitionSerialization {
  private final CalltargetIndexProvider myCalltargetIndexProvider;
  private final List<Binding> myBindings = new ArrayList<>();  // de Bruijn indices
  private final Map<Binding, Integer> myBindingsMap = new HashMap<>();
  private final List<LevelBinding> myLvlBindings = new ArrayList<>();  // de Bruijn indices for level bindings
  private final Map<LevelBinding, Integer> myLvlBindingsMap = new HashMap<>();
  private final SerializeVisitor myVisitor = new SerializeVisitor();

  DefinitionSerialization(CalltargetIndexProvider calltargetIndexProvider) {
    myCalltargetIndexProvider = calltargetIndexProvider;
  }


  // Bindings

  private RollbackBindings checkpointBindings() {
    return new RollbackBindings(myBindings.size(), myLvlBindings.size());
  }

  private class RollbackBindings implements AutoCloseable {
    private final int myTargetSize;
    private final int myLvlTargetSize;

    private RollbackBindings(int targetSize, int lvlTargetSize) {
      myTargetSize = targetSize;
      myLvlTargetSize = lvlTargetSize;
    }

    @Override
    public void close() {
      for (int i = myBindings.size() - 1; i >= myTargetSize; i--) {
        myBindingsMap.remove(myBindings.remove(i));
      }
      for (int i = myLvlBindings.size() - 1; i >= myLvlTargetSize; i--) {
        myLvlBindingsMap.remove(myLvlBindings.remove(i));
      }
    }
  }

  private int registerBinding(Binding binding) {
    int index = myBindings.size();
    myBindings.add(binding);
    myBindingsMap.put(binding, index);
    return index;
  }

  private int registerLevelBinding(LevelBinding binding) {
    int index = myLvlBindings.size();
    myLvlBindings.add(binding);
    myLvlBindingsMap.put(binding, index);
    return index;
  }

  ExpressionProtos.Binding.TypedBinding createTypedBinding(TypedBinding binding) {
    ExpressionProtos.Binding.TypedBinding.Builder builder = ExpressionProtos.Binding.TypedBinding.newBuilder();
    if (binding.getName() != null) {
      builder.setName(binding.getName());
    }
    builder.setType(writeType(binding.getType()));
    registerBinding(binding);
    return builder.build();
  }

  ExpressionProtos.Binding.LevelBinding createLevelBinding(LevelBinding binding) {
    ExpressionProtos.Binding.LevelBinding.Builder builder = ExpressionProtos.Binding.LevelBinding.newBuilder();
    if (binding.getName() != null) {
      builder.setName(binding.getName());
    }
    builder.setType(binding.getType() == LevelVariable.LvlType.PLVL ? ExpressionProtos.Binding.LevelBinding.LvlType.PLVL : ExpressionProtos.Binding.LevelBinding.LvlType.HLVL);
    registerLevelBinding(binding);
    return builder.build();
  }

  private int writeBindingRef(Variable binding) {
    if (binding == null) {
      return 0;
    } else {
      Integer index = binding instanceof Binding ? myBindingsMap.get(binding) : myLvlBindingsMap.get(binding);
      return index + 1;  // zero is reserved for null
    }
  }


  // Patterns

  DefinitionProtos.Definition.DataData.Constructor.Patterns writePatterns(Patterns patterns) {
    DefinitionProtos.Definition.DataData.Constructor.Patterns.Builder builder = DefinitionProtos.Definition.DataData.Constructor.Patterns.newBuilder();
    for (PatternArgument patternArg : patterns.getPatterns()) {
      DefinitionProtos.Definition.DataData.Constructor.PatternArgument.Builder paBuilder = DefinitionProtos.Definition.DataData.Constructor.PatternArgument.newBuilder();
      paBuilder.setNotExplicit(!patternArg.isExplicit());
      paBuilder.setHidden(patternArg.isHidden());
      paBuilder.setPattern(writePattern(patternArg.getPattern()));
      builder.addPatternArgument(paBuilder.build());
    }
    return builder.build();
  }

  private DefinitionProtos.Definition.DataData.Constructor.Pattern writePattern(Pattern pattern) {
    DefinitionProtos.Definition.DataData.Constructor.Pattern.Builder builder = DefinitionProtos.Definition.DataData.Constructor.Pattern.newBuilder();
    if (pattern instanceof NamePattern) {
      builder.setName(
          DefinitionProtos.Definition.DataData.Constructor.Pattern.Name.newBuilder()
            .setVar(writeParameter(pattern.getParameters()))
      );
    } else if (pattern instanceof AnyConstructorPattern) {
      builder.setAnyConstructor(
          DefinitionProtos.Definition.DataData.Constructor.Pattern.AnyConstructor.newBuilder()
              .setVar(writeParameter(pattern.getParameters()))
      );
    } else if (pattern instanceof ConstructorPattern) {
      DefinitionProtos.Definition.DataData.Constructor.Pattern.ConstructorRef.Builder pBuilder = DefinitionProtos.Definition.DataData.Constructor.Pattern.ConstructorRef.newBuilder();
      pBuilder.setConstructorRef(myCalltargetIndexProvider.getDefIndex(((ConstructorPattern) pattern).getConstructor()));
      pBuilder.setPatterns(writePatterns(((ConstructorPattern) pattern).getPatterns()));
      builder.setConstructor(pBuilder.build());
    } else {
      throw new IllegalArgumentException();
    }
    return builder.build();
  }


  // Sorts and levels

  private LevelProtos.Level writeLevel(Level level) {
    // Level.INFINITY should be read with great care
    LevelProtos.Level.Builder builder = LevelProtos.Level.newBuilder();
    if (level.getVar() == null || level.getVar() instanceof LevelBinding) {
      builder.setBindingRef(writeBindingRef(level.getVar()));
    } else {
      throw new IllegalStateException();
    }
    builder.setConstant(level.getConstant());
    return builder.build();
  }

  private LevelProtos.LevelMax writeLevelMax(LevelMax levelMax) {
    // This will store LevelMax.INFINITY as a singleton list with Level.INFINITY in it
    LevelProtos.LevelMax.Builder builder = LevelProtos.LevelMax.newBuilder();
    for (Level level : levelMax.toListOfLevels()) {
      builder.addLevel(writeLevel(level));
    }
    return builder.build();
  }

  private LevelProtos.Sort writeSort(Sort sort) {
    LevelProtos.Sort.Builder builder = LevelProtos.Sort.newBuilder();
    builder.setPLevel(writeLevel(sort.getPLevel()));
    builder.setHLevel(writeLevel(sort.getHLevel()));
    return builder.build();
  }

  private LevelProtos.SortMax writeSortMax(SortMax sort) {
    LevelProtos.SortMax.Builder builder = LevelProtos.SortMax.newBuilder();
    builder.setPLevel(writeLevelMax(sort.getPLevel()));
    builder.setHLevel(writeLevelMax(sort.getHLevel()));
    return builder.build();
  }

  private List<LevelProtos.Level> writePolyArguments(LevelArguments args) {
    List<LevelProtos.Level> out = new ArrayList<>();
    for (Level level : args.getLevels()) {
      out.add(writeLevel(level));
    }
    return out;
  }


  // Parameters

  List<ExpressionProtos.Telescope> writeParameters(DependentLink link) {
    List<ExpressionProtos.Telescope> out = new ArrayList<>();
    for (; link.hasNext(); link = link.getNext()) {
      ExpressionProtos.Telescope.Builder tBuilder = ExpressionProtos.Telescope.newBuilder();
      List<String> names = new ArrayList<>();
      TypedDependentLink typed = link.getNextTyped(names);
      List<String> fixedNames = new ArrayList<>(names.size());
      for (String name : names) {
        if (name != null && name.isEmpty()) {
          throw new IllegalArgumentException();
        }
        fixedNames.add(name == null ? "" : name);
      }
      tBuilder.addAllName(fixedNames);
      tBuilder.setIsNotExplicit(!typed.isExplicit());
      tBuilder.setType(writeType(typed.getType()));
      for (; link != typed; link = link.getNext()) {
        registerBinding(link);
      }
      registerBinding(typed);
      out.add(tBuilder.build());
    }
    return out;
  }

  ExpressionProtos.SingleParameter writeParameter(DependentLink link) {
    ExpressionProtos.SingleParameter.Builder builder = ExpressionProtos.SingleParameter.newBuilder();
    if (link instanceof TypedDependentLink) {
      if (link.getName() != null) {
        builder.setName(link.getName());
      }
      builder.setIsNotExplicit(!link.isExplicit());
      builder.setType(writeType(link.getType()));
    } else {
      throw new IllegalStateException();
    }
    registerBinding(link);
    return builder.build();
  }


  // FieldSet

  ExpressionProtos.FieldSet writeFieldSet(FieldSet fieldSet) {
    ExpressionProtos.FieldSet.Builder builder = ExpressionProtos.FieldSet.newBuilder();
    for (ClassField classField : fieldSet.getFields()) {
      builder.addClassFieldRef(myCalltargetIndexProvider.getDefIndex(classField));
    }
    for (Map.Entry<ClassField, FieldSet.Implementation> impl : fieldSet.getImplemented()) {
      ExpressionProtos.FieldSet.Implementation.Builder iBuilder = ExpressionProtos.FieldSet.Implementation.newBuilder();
      if (impl.getValue().thisParam != null) {
        iBuilder.setThisParam(writeParameter(impl.getValue().thisParam));
      }
      iBuilder.setTerm(writeExpr(impl.getValue().term));
      builder.putImplementations(myCalltargetIndexProvider.getDefIndex(impl.getKey()), iBuilder.build());
    }
    return builder.build();
  }


  // Types, Expressions and ElimTrees

  ExpressionProtos.Expression writeExpr(Expression expr) {
    return expr.accept(myVisitor, null);
  }

  ExpressionProtos.Type writeType(TypeMax type) {
    ExpressionProtos.Type.Builder builder = ExpressionProtos.Type.newBuilder();
    if (type instanceof PiUniverseType) {
      ExpressionProtos.Type.PiUniverse.Builder piUniverse = ExpressionProtos.Type.PiUniverse.newBuilder();
      piUniverse.addAllParam(writeParameters(type.getPiParameters()));
      piUniverse.setSorts(writeSortMax(((PiUniverseType) type).getSorts()));
      builder.setPiUniverse(piUniverse);
    } else if (type instanceof Expression) {
      builder.setExpr(writeExpr((Expression) type));
    } else {
      throw new IllegalStateException();
    }
    return builder.build();
  }

  ExpressionProtos.ElimTreeNode writeElimTree(ElimTreeNode elimTree) {
    return elimTree.accept(myVisitor, null);
  }


  private class SerializeVisitor implements ExpressionVisitor<Void, ExpressionProtos.Expression>, ElimTreeNodeVisitor<Void, ExpressionProtos.ElimTreeNode> {
    @Override
    public ExpressionProtos.Expression visitApp(AppExpression expr, Void params) {
      ExpressionProtos.Expression.App.Builder builder = ExpressionProtos.Expression.App.newBuilder();
      builder.setFunction(expr.getFunction().accept(this, null));
      for (Expression arg : expr.getArguments()) {
        builder.addArgument(arg.accept(this, null));
      }
      return ExpressionProtos.Expression.newBuilder().setApp(builder).build();
    }

    @Override
    public ExpressionProtos.Expression visitFunCall(FunCallExpression expr, Void params) {
      ExpressionProtos.Expression.FunCall.Builder builder = ExpressionProtos.Expression.FunCall.newBuilder();
      builder.setFunRef(myCalltargetIndexProvider.getDefIndex(expr.getDefinition()));
      builder.addAllPolyArguments(writePolyArguments(expr.getPolyArguments()));
      for (Expression arg : expr.getDefCallArguments()) {
        builder.addArgument(arg.accept(this, null));
      }
      return ExpressionProtos.Expression.newBuilder().setFunCall(builder).build();
    }

    @Override
    public ExpressionProtos.Expression visitConCall(ConCallExpression expr, Void params) {
      ExpressionProtos.Expression.ConCall.Builder builder = ExpressionProtos.Expression.ConCall.newBuilder();
      builder.setConstructorRef(myCalltargetIndexProvider.getDefIndex(expr.getDefinition()));
      builder.addAllPolyArguments(writePolyArguments(expr.getPolyArguments()));
      for (Expression arg : expr.getDataTypeArguments()) {
        builder.addDatatypeArgument(arg.accept(this, null));
      }
      for (Expression arg : expr.getDefCallArguments()) {
        builder.addArgument(arg.accept(this, null));
      }
      return ExpressionProtos.Expression.newBuilder().setConCall(builder).build();
    }

    @Override
    public ExpressionProtos.Expression visitDataCall(DataCallExpression expr, Void params) {
      ExpressionProtos.Expression.DataCall.Builder builder = ExpressionProtos.Expression.DataCall.newBuilder();
      builder.setDataRef(myCalltargetIndexProvider.getDefIndex(expr.getDefinition()));
      builder.addAllPolyArguments(writePolyArguments(expr.getPolyArguments()));
      for (Expression arg : expr.getDefCallArguments()) {
        builder.addArgument(arg.accept(this, null));
      }
      return ExpressionProtos.Expression.newBuilder().setDataCall(builder).build();
    }

    private ExpressionProtos.Expression.ClassCall writeClassCall(ClassCallExpression expr) {
      ExpressionProtos.Expression.ClassCall.Builder builder = ExpressionProtos.Expression.ClassCall.newBuilder();
      builder.setClassRef(myCalltargetIndexProvider.getDefIndex(expr.getDefinition()));
      builder.addAllPolyArguments(writePolyArguments(expr.getPolyArguments()));
      builder.setFieldSet(writeFieldSet(expr.getFieldSet()));
      return builder.build();
    }

    @Override
    public ExpressionProtos.Expression visitClassCall(ClassCallExpression expr, Void params) {
      return ExpressionProtos.Expression.newBuilder().setClassCall(writeClassCall(expr)).build();
    }

    @Override
    public ExpressionProtos.Expression visitReference(ReferenceExpression expr, Void params) {
      ExpressionProtos.Expression.Reference.Builder builder = ExpressionProtos.Expression.Reference.newBuilder();
      builder.setBindingRef(writeBindingRef(expr.getBinding()));
      return ExpressionProtos.Expression.newBuilder().setReference(builder).build();
    }

    @Override
    public ExpressionProtos.Expression visitInferenceReference(InferenceReferenceExpression expr, Void params) {
      throw new IllegalStateException();
    }

    @Override
    public ExpressionProtos.Expression visitLam(LamExpression expr, Void params) {
      ExpressionProtos.Expression.Lam.Builder builder = ExpressionProtos.Expression.Lam.newBuilder();
      builder.addAllParam(writeParameters(expr.getParameters()));
      builder.setBody(expr.getBody().accept(this, null));
      return ExpressionProtos.Expression.newBuilder().setLam(builder).build();
    }

    @Override
    public ExpressionProtos.Expression visitPi(PiExpression expr, Void params) {
      ExpressionProtos.Expression.Pi.Builder builder = ExpressionProtos.Expression.Pi.newBuilder();
      builder.addAllParam(writeParameters(expr.getParameters()));
      builder.setCodomain(expr.getCodomain().accept(this, null));
      return ExpressionProtos.Expression.newBuilder().setPi(builder).build();
    }

    @Override
    public ExpressionProtos.Expression visitUniverse(UniverseExpression expr, Void params) {
      ExpressionProtos.Expression.Universe.Builder builder = ExpressionProtos.Expression.Universe.newBuilder();
      builder.setSort(writeSort(expr.getSort()));
      return ExpressionProtos.Expression.newBuilder().setUniverse(builder).build();
    }

    @Override
    public ExpressionProtos.Expression visitError(ErrorExpression expr, Void params) {
      ExpressionProtos.Expression.Error.Builder builder = ExpressionProtos.Expression.Error.newBuilder();
      if (expr.getExpr() != null) {
        builder.setExpression(expr.getExpr().accept(this, null));
      }
      return ExpressionProtos.Expression.newBuilder().setError(builder).build();
    }

    @Override
    public ExpressionProtos.Expression visitTuple(TupleExpression expr, Void params) {
      ExpressionProtos.Expression.Tuple.Builder builder = ExpressionProtos.Expression.Tuple.newBuilder();
      for (Expression field : expr.getFields()) {
        builder.addField(field.accept(this, null));
      }
      builder.setType(writeSigma(expr.getType()));
      return ExpressionProtos.Expression.newBuilder().setTuple(builder).build();
    }

    private ExpressionProtos.Expression.Sigma writeSigma(SigmaExpression sigma) {
      ExpressionProtos.Expression.Sigma.Builder builder = ExpressionProtos.Expression.Sigma.newBuilder();
      builder.addAllParam(writeParameters(sigma.getParameters()));
      return builder.build();
    }

    @Override
    public ExpressionProtos.Expression visitSigma(SigmaExpression expr, Void params) {
      return ExpressionProtos.Expression.newBuilder().setSigma(writeSigma(expr)).build();
    }

    @Override
    public ExpressionProtos.Expression visitProj(ProjExpression expr, Void params) {
      ExpressionProtos.Expression.Proj.Builder builder = ExpressionProtos.Expression.Proj.newBuilder();
      builder.setExpression(expr.getExpression().accept(this, null));
      builder.setField(expr.getField());
      return ExpressionProtos.Expression.newBuilder().setProj(builder).build();
    }

    @Override
    public ExpressionProtos.Expression visitNew(NewExpression expr, Void params) {
      ExpressionProtos.Expression.New.Builder builder = ExpressionProtos.Expression.New.newBuilder();
      builder.setClassCall(writeClassCall(expr.getExpression()));
      return ExpressionProtos.Expression.newBuilder().setNew(builder).build();
    }

    @Override
    public ExpressionProtos.Expression visitLet(LetExpression letExpression, Void params) {
      ExpressionProtos.Expression.Let.Builder builder = ExpressionProtos.Expression.Let.newBuilder();
      for (LetClause letClause : letExpression.getClauses()) {
        ExpressionProtos.Expression.Let.Clause.Builder cBuilder = ExpressionProtos.Expression.Let.Clause.newBuilder();
        cBuilder.setName(letClause.getName());
        cBuilder.addAllParam(writeParameters(letClause.getParameters()));
        cBuilder.setResultType(letClause.getResultType().accept(this, null));
        cBuilder.setElimTree(writeElimTree(letClause.getElimTree()));
        builder.addClause(cBuilder);
        registerBinding(letClause);
      }
      builder.setExpression(letExpression.getExpression().accept(this, null));
      return ExpressionProtos.Expression.newBuilder().setLet(builder).build();
    }

    @Override
    public ExpressionProtos.Expression visitOfType(OfTypeExpression expr, Void params) {
      throw new IllegalStateException();
    }

    @Override
    public ExpressionProtos.Expression visitFieldCall(FieldCallExpression expr, Void params) {
      ExpressionProtos.Expression.FieldCall.Builder builder = ExpressionProtos.Expression.FieldCall.newBuilder();
      builder.setFieldRef(myCalltargetIndexProvider.getDefIndex(expr.getDefinition()));
      builder.addAllPolyArguments(writePolyArguments(expr.getPolyArguments()));
      builder.setExpression(expr.getExpression().accept(this, null));
      return ExpressionProtos.Expression.newBuilder().setFieldCall(builder).build();
    }


    @Override
    public ExpressionProtos.ElimTreeNode visitBranch(BranchElimTreeNode branchNode, Void params) {
      ExpressionProtos.ElimTreeNode.Branch.Builder builder = ExpressionProtos.ElimTreeNode.Branch.newBuilder();

      builder.setReferenceRef(writeBindingRef(branchNode.getReference()));
      for (Binding binding : branchNode.getContextTail()) {
        builder.addContextTailItemRef(writeBindingRef(binding));
      }
      for (ConstructorClause clause : branchNode.getConstructorClauses()) {
        ExpressionProtos.ElimTreeNode.ConstructorClause.Builder ccBuilder = ExpressionProtos.ElimTreeNode.ConstructorClause.newBuilder();

        for (LevelBinding polyVar : clause.getPolyParams()) {
          ccBuilder.addPolyParam(createLevelBinding(polyVar));
        }
        ccBuilder.addAllParam(writeParameters(clause.getParameters()));
        for (TypedBinding binding : clause.getTailBindings()) {
          ccBuilder.addTailBinding(createTypedBinding(binding));
        }
        ccBuilder.setChild(writeElimTree(clause.getChild()));

        builder.putConstructorClauses(myCalltargetIndexProvider.getDefIndex(clause.getConstructor()), ccBuilder.build());
      }
      if (branchNode.getOtherwiseClause() != null) {
        builder.setOtherwiseClause(writeElimTree(branchNode.getOtherwiseClause().getChild()));
      }
      return ExpressionProtos.ElimTreeNode.newBuilder().setBranch(builder).build();
    }

    @Override
    public ExpressionProtos.ElimTreeNode visitLeaf(LeafElimTreeNode leafNode, Void params) {
      ExpressionProtos.ElimTreeNode.Leaf.Builder builder = ExpressionProtos.ElimTreeNode.Leaf.newBuilder();
      builder.setArrowLeft(Abstract.Definition.Arrow.LEFT.equals(leafNode.getArrow()));
      for (Binding binding : leafNode.getMatched()) {
        builder.addMatchedRef(writeBindingRef(binding));
      }
      builder.setExpr(leafNode.getExpression().accept(this, null));
      return ExpressionProtos.ElimTreeNode.newBuilder().setLeaf(builder).build();
    }

    @Override
    public ExpressionProtos.ElimTreeNode visitEmpty(EmptyElimTreeNode emptyNode, Void params) {
      return ExpressionProtos.ElimTreeNode.newBuilder().setEmpty(ExpressionProtos.ElimTreeNode.Empty.newBuilder()).build();
    }
  }
}

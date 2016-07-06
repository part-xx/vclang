package com.jetbrains.jetpad.vclang.naming.oneshot.visitor;

import com.jetbrains.jetpad.vclang.error.ErrorReporter;
import com.jetbrains.jetpad.vclang.error.GeneralError;
import com.jetbrains.jetpad.vclang.naming.NameResolver;
import com.jetbrains.jetpad.vclang.naming.namespace.DynamicNamespaceProvider;
import com.jetbrains.jetpad.vclang.naming.namespace.ModuleNamespace;
import com.jetbrains.jetpad.vclang.naming.namespace.StaticNamespaceProvider;
import com.jetbrains.jetpad.vclang.naming.oneshot.ResolveListener;
import com.jetbrains.jetpad.vclang.naming.scope.MergeScope;
import com.jetbrains.jetpad.vclang.naming.scope.Scope;
import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.definition.Referable;
import com.jetbrains.jetpad.vclang.term.statement.visitor.AbstractStatementVisitor;
import com.jetbrains.jetpad.vclang.typechecking.error.TypeCheckingError;

import java.util.List;

public class StatementResolveNameVisitor implements AbstractStatementVisitor<DefinitionResolveNameVisitor.Flag, Object> {
  private final StaticNamespaceProvider myStaticNsProvider;
  private final DynamicNamespaceProvider myDynamicNsProvider;
  private final List<String> myContext;
  private final NameResolver myNameResolver;
  private final ErrorReporter myErrorReporter;
  private final ResolveListener myResolveListener;
  private Scope myScope;

  public StatementResolveNameVisitor(StaticNamespaceProvider staticNsProvider, DynamicNamespaceProvider dynamicNsProvider, NameResolver nameResolver, ErrorReporter errorReporter, Scope parentScope, List<String> context, ResolveListener resolveListener) {
    myStaticNsProvider = staticNsProvider;
    myDynamicNsProvider = dynamicNsProvider;
    myNameResolver = nameResolver;
    myErrorReporter = errorReporter;
    myScope = parentScope;
    myContext = context;
    myResolveListener = resolveListener;
  }

  @Override
  public Void visitDefine(Abstract.DefineStatement stat, DefinitionResolveNameVisitor.Flag flag) {
    if (stat.getStaticMod() != Abstract.DefineStatement.StaticMod.STATIC && flag == DefinitionResolveNameVisitor.Flag.MUST_BE_STATIC) {
      myErrorReporter.report(new GeneralError("Non-static definition in a static context", stat));
      return null;
    } else if (stat.getStaticMod() == Abstract.DefineStatement.StaticMod.STATIC && flag == DefinitionResolveNameVisitor.Flag.MUST_BE_DYNAMIC) {
      myErrorReporter.report(new TypeCheckingError("Static definitions are not allowed in this context", stat));
      return null;
    } else if (stat.getStaticMod() == Abstract.DefineStatement.StaticMod.STATIC && stat.getDefinition() instanceof Abstract.AbstractDefinition) {
      myErrorReporter.report(new TypeCheckingError("Abstract definitions cannot be static", stat));
      return null;
    }
    DefinitionResolveNameVisitor visitor = new DefinitionResolveNameVisitor(myStaticNsProvider, myDynamicNsProvider, myScope, myContext, myNameResolver, myErrorReporter, myResolveListener);
    stat.getDefinition().accept(visitor, stat.getStaticMod() == Abstract.DefineStatement.StaticMod.STATIC);
    return null;
  }

  @Override
  public Void visitNamespaceCommand(Abstract.NamespaceCommandStatement stat, DefinitionResolveNameVisitor.Flag flag) {
    if (flag == DefinitionResolveNameVisitor.Flag.MUST_BE_DYNAMIC) {
      myErrorReporter.report(new TypeCheckingError("Namespace commands are not allowed in this context", stat));
      return null;
    }

    if (Abstract.NamespaceCommandStatement.Kind.EXPORT.equals(stat.getKind())) {
      throw new UnsupportedOperationException();
    }

    final Referable referredClass;
    if (stat.getModulePath() == null) {
      if (stat.getPath().isEmpty()) {
        myErrorReporter.report(new GeneralError("Structure error: empty namespace command", stat));  // FIXME[error]: report proper
        return null;
      }
      referredClass = myNameResolver.resolveDefinition(myScope, stat.getPath());
    } else {
      ModuleNamespace moduleNamespace = myNameResolver.resolveModuleNamespace(stat.getModulePath());
      Abstract.ClassDefinition moduleClass = moduleNamespace != null ? moduleNamespace.getRegisteredClass() : null;
      if (moduleClass == null) {
        myErrorReporter.report(new GeneralError("Module not found: " + stat.getModulePath(), stat));  // FIXME[error]: report proper
        return null;
      }
      if (stat.getPath().isEmpty()) {
        referredClass = moduleNamespace.getRegisteredClass();
      } else {
        referredClass = myNameResolver.resolveDefinition(myNameResolver.staticNamespaceFor(moduleClass), stat.getPath());
      }
    }

    if (referredClass == null) {
      myErrorReporter.report(new GeneralError("Class not found", stat));  // FIXME[error]: report proper
      return null;
    }

    if (stat.getKind().equals(Abstract.NamespaceCommandStatement.Kind.OPEN)) {
      myScope = new MergeScope(myScope, myNameResolver.staticNamespaceFor(referredClass));
    }

    return null;
  }

  @Override
  public Object visitDefaultStaticCommand(Abstract.DefaultStaticStatement stat, DefinitionResolveNameVisitor.Flag params) {
    return null;
  }

  /*
  private void processNamespaceCommand(NamespaceMember member, boolean export, boolean remove, Abstract.SourceNode sourceNode) {
    boolean ok;
    if (export) {
      ok = myNamespace.addMember(member) == null;
    } else
    if (remove) {
      ok = myPrivateNameResolver.locateName(member.namespace.getName()) != null;
      myPrivateNameResolver.remove(member.namespace.getName());
    } else {
      ok = myPrivateNameResolver.locateName(member.namespace.getName()) == null;
      myPrivateNameResolver.add(member);
    }

    if (!ok) {
      GeneralError error = new NameDefinedError(!remove, sourceNode, member.namespace.getName(), null);
      error.setLevel(GeneralError.Level.WARNING);
      myErrorReporter.report(error);
    }
  }
  */

  public Scope getCurrentScope() {
    return myScope;
  }
}
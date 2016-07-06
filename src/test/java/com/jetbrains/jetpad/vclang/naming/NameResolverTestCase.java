package com.jetbrains.jetpad.vclang.naming;

import com.jetbrains.jetpad.vclang.error.GeneralError;
import com.jetbrains.jetpad.vclang.error.ListErrorReporter;
import com.jetbrains.jetpad.vclang.naming.namespace.*;
import com.jetbrains.jetpad.vclang.naming.oneshot.visitor.DefinitionResolveNameVisitor;
import com.jetbrains.jetpad.vclang.naming.oneshot.visitor.ExpressionResolveNameVisitor;
import com.jetbrains.jetpad.vclang.naming.scope.Scope;
import com.jetbrains.jetpad.vclang.naming.scope.SubScope;
import com.jetbrains.jetpad.vclang.term.*;
import com.jetbrains.jetpad.vclang.term.context.binding.Binding;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static com.jetbrains.jetpad.vclang.parser.ParserTestCase.*;
import static com.jetbrains.jetpad.vclang.util.TestUtil.assertErrorListSize;

public class NameResolverTestCase {
  public static final ModuleNamespaceProvider DEFAULT_MODULE_NS_PROVIDER = new SimpleModuleNamespaceProvider();
  public static final StaticNamespaceProvider DEFAULT_STATIC_NS_PROVIDER = new SimpleStaticNamespaceProvider();
  public static final DynamicNamespaceProvider DEFAULT_DYNAMIC_NS_PROVIDER = new SimpleDynamicNamespaceProvider();
  public static final NameResolver DEFAULT_NAME_RESOLVER = new NameResolver(DEFAULT_MODULE_NS_PROVIDER, DEFAULT_STATIC_NS_PROVIDER);
  public static final Scope INITIAL_SCOPE = new SubScope(Preprelude.PRE_PRELUDE, Prelude.PRELUDE);

  public static Collection<? extends GeneralError> resolveNamesExpr(Scope parentScope, List<String> context, Concrete.Expression expression) {
    ListErrorReporter errorReporter = new ListErrorReporter();
    expression.accept(new ExpressionResolveNameVisitor(parentScope, context, DEFAULT_NAME_RESOLVER, errorReporter, new ConcreteResolveListener()), null);
    return errorReporter.getErrorList();
  }

  public static Collection<? extends GeneralError> resolveNamesExpr(Concrete.Expression expression) {
    return resolveNamesExpr(INITIAL_SCOPE, new ArrayList<String>(), expression);
  }

  public static Concrete.Expression resolveNamesExpr(Scope parentScope, List<String> context, String text, int errors) {
    Concrete.Expression result = parseExpr(text);
    Collection<? extends GeneralError> errorList = resolveNamesExpr(parentScope, context, result);
    assertErrorListSize(errorList, errors);
    return result;
  }

  public static Concrete.Expression resolveNamesExpr(String text, int errors) {
    Concrete.Expression result = parseExpr(text);
    Collection<? extends GeneralError> errorList = resolveNamesExpr(result);
    assertErrorListSize(errorList, errors);
    return result;
  }

  public static Concrete.Expression resolveNamesExpr(Scope parentScope, String text, int errors) {
    return resolveNamesExpr(parentScope, new ArrayList<String>(), text, errors);
  }

  public static Concrete.Expression resolveNamesExpr(Scope parentScope, String text) {
    return resolveNamesExpr(parentScope, text, 0);
  }

  public static Concrete.Expression resolveNamesExpr(List<Binding> context, String text) {
    List<String> names = new ArrayList<>(context.size());
    for (Binding binding : context) {
      names.add(binding.getName());
    }
    return resolveNamesExpr(INITIAL_SCOPE, names, text, 0);
  }

  public static Concrete.Expression resolveNamesExpr(String text) {
    return resolveNamesExpr(text, 0);
  }

  public static Collection<? extends GeneralError> resolveNamesDef(Concrete.Definition definition) {
    ListErrorReporter errorReporter = new ListErrorReporter();
    DefinitionResolveNameVisitor visitor = new DefinitionResolveNameVisitor(DEFAULT_STATIC_NS_PROVIDER, DEFAULT_DYNAMIC_NS_PROVIDER,
        new SubScope(INITIAL_SCOPE, new SimpleNamespace(definition)), DEFAULT_NAME_RESOLVER, errorReporter, new ConcreteResolveListener());
    definition.accept(visitor, null);
    return errorReporter.getErrorList();
  }

  public static void resolveNamesDef(Concrete.Definition definition, int errors) {
    Collection<? extends GeneralError> errorList = resolveNamesDef(definition);
    assertErrorListSize(errorList, errors);
  }

  public static Concrete.Definition resolveNamesDef(String text, int errors) {
    Concrete.Definition result = parseDef(text);
    resolveNamesDef(result, errors);
    return result;
  }

  public static Concrete.Definition resolveNamesDef(String text) {
    return resolveNamesDef(text, 0);
  }

  public static void resolveNamesClass(Concrete.ClassDefinition classDefinition, int errors) {
    resolveNamesDef(classDefinition, errors);
  }

  public static Concrete.ClassDefinition resolveNamesClass(String name, String text, int errors) {
    Concrete.ClassDefinition classDefinition = parseClass(name, text);
    resolveNamesClass(classDefinition, errors);
    return classDefinition;
  }

  public static Concrete.ClassDefinition resolveNamesClass(String text, int errors) {
    return resolveNamesClass("test", text, errors);
  }

  public static Concrete.ClassDefinition resolveNamesClass(String name, String text) {
    return resolveNamesClass(name, text, 0);
  }
}
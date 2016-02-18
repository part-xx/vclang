package com.jetbrains.jetpad.vclang.typechecking;

import com.jetbrains.jetpad.vclang.module.RootModule;
import com.jetbrains.jetpad.vclang.term.Concrete;
import com.jetbrains.jetpad.vclang.term.context.binding.Binding;
import com.jetbrains.jetpad.vclang.term.definition.ClassDefinition;
import com.jetbrains.jetpad.vclang.term.definition.Definition;
import com.jetbrains.jetpad.vclang.term.definition.ResolvedName;
import com.jetbrains.jetpad.vclang.term.definition.visitor.DefinitionCheckTypeVisitor;
import com.jetbrains.jetpad.vclang.term.expr.Expression;
import com.jetbrains.jetpad.vclang.term.expr.visitor.CheckTypeVisitor;
import com.jetbrains.jetpad.vclang.typechecking.error.reporter.ErrorReporter;
import com.jetbrains.jetpad.vclang.typechecking.error.reporter.ListErrorReporter;

import java.util.ArrayList;
import java.util.List;

import static com.jetbrains.jetpad.vclang.parser.ParserTestCase.parseClass;
import static com.jetbrains.jetpad.vclang.typechecking.nameresolver.NameResolverTestCase.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class TypeCheckingTestCase {
  public static CheckTypeVisitor.Result typeCheckExpr(List<Binding> context, Concrete.Expression expression, Expression expectedType, ErrorReporter errorReporter) {
    return new CheckTypeVisitor.Builder(context, errorReporter).build().checkType(expression, expectedType);
  }

  public static CheckTypeVisitor.Result typeCheckExpr(Concrete.Expression expression, Expression expectedType, ErrorReporter errorReporter) {
    return typeCheckExpr(new ArrayList<Binding>(), expression, expectedType, errorReporter);
  }

  public static CheckTypeVisitor.Result typeCheckExpr(List<Binding> context, Concrete.Expression expression, Expression expectedType, int errors) {
    ListErrorReporter errorReporter = new ListErrorReporter();
    CheckTypeVisitor.Result result = typeCheckExpr(context, expression, expectedType, errorReporter);
    assertEquals(errorReporter.getErrorList().toString(), errors, errorReporter.getErrorList().size());
    return result;
  }

  public static CheckTypeVisitor.Result typeCheckExpr(Concrete.Expression expression, Expression expectedType, int errors) {
    return typeCheckExpr(new ArrayList<Binding>(), expression, expectedType, errors);
  }

  public static CheckTypeVisitor.Result typeCheckExpr(List<Binding> context, String text, Expression expectedType, ErrorReporter errorReporter) {
    return typeCheckExpr(context, resolveNamesExpr(text), expectedType, errorReporter);
  }

  public static CheckTypeVisitor.Result typeCheckExpr(String text, Expression expectedType, ErrorReporter errorReporter) {
    return typeCheckExpr(resolveNamesExpr(text), expectedType, errorReporter);
  }

  public static CheckTypeVisitor.Result typeCheckExpr(List<Binding> context, String text, Expression expectedType, int errors) {
    return typeCheckExpr(context, resolveNamesExpr(text), expectedType, errors);
  }

  public static CheckTypeVisitor.Result typeCheckExpr(String text, Expression expectedType, int errors) {
    return typeCheckExpr(resolveNamesExpr(text), expectedType, errors);
  }

  public static CheckTypeVisitor.Result typeCheckExpr(List<Binding> context, String text, Expression expectedType) {
    return typeCheckExpr(context, resolveNamesExpr(text), expectedType, 0);
  }

  public static CheckTypeVisitor.Result typeCheckExpr(String text, Expression expectedType) {
    return typeCheckExpr(resolveNamesExpr(text), expectedType, 0);
  }

  public static Definition typeCheckDef(Concrete.Definition definition, int errors) {
    ListErrorReporter errorReporter = new ListErrorReporter();
    DefinitionCheckTypeVisitor visitor = new DefinitionCheckTypeVisitor(RootModule.ROOT.getChild("test"), errorReporter);
    visitor.setNamespaceMember(RootModule.ROOT.getChild("test").getMember(definition.getName()));
    Definition result = definition.accept(visitor, null);
    assertEquals(errorReporter.getErrorList().toString(), errors, errorReporter.getErrorList().size());
    return result;
  }

  public static Definition typeCheckDef(String text, int errors) {
    return typeCheckDef(resolveNamesDef(text), errors);
  }

  public static Definition typeCheckDef(String text) {
    return typeCheckDef(text, 0);
  }

  public static ClassDefinition typeCheckClass(Concrete.ClassDefinition classDefinition, int errors) {
    ListErrorReporter errorReporter = new ListErrorReporter();
    TypecheckingOrdering.typecheck(new ResolvedName(RootModule.ROOT, classDefinition.getName()), errorReporter);
    if (errors >= 0) {
      assertEquals(errorReporter.getErrorList().toString(), errors, errorReporter.getErrorList().size());
    } else {
      assertFalse(errorReporter.getErrorList().toString(), errorReporter.getErrorList().isEmpty());
    }
    return (ClassDefinition) RootModule.ROOT.getDefinition(classDefinition.getName());
  }

  public static ClassDefinition typeCheckClass(String text, int errors) {
    Concrete.ClassDefinition classDefinition = parseClass("test", text);
    resolveNamesClass(classDefinition, 0);
    RootModule.ROOT.addAbstractDefinition(classDefinition);
    return typeCheckClass(classDefinition, errors);
  }

  public static ClassDefinition typeCheckClass(String text) {
    return typeCheckClass(text, 0);
  }
}

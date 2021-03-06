package com.jetbrains.jetpad.vclang.frontend.parser;

import com.jetbrains.jetpad.vclang.VclangTestCase;
import com.jetbrains.jetpad.vclang.module.ModulePath;
import com.jetbrains.jetpad.vclang.module.source.SourceId;
import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.frontend.Concrete;
import com.jetbrains.jetpad.vclang.frontend.ConcreteExpressionFactory;
import com.jetbrains.jetpad.vclang.frontend.AbstractCompareVisitor;
import org.antlr.v4.runtime.*;

import static org.junit.Assert.assertThat;

public abstract class ParserTestCase extends VclangTestCase {
  private static final SourceId SOURCE_ID = new SourceId() {
    @Override
    public ModulePath getModulePath() {
      return ModulePath.moduleName(toString());
    }
    @Override
    public String toString() {
      return "$TestCase$";
    }
  };

  private VcgrammarParser _parse(String text) {
    ANTLRInputStream input = new ANTLRInputStream(text);
    VcgrammarLexer lexer = new VcgrammarLexer(input);
    lexer.removeErrorListeners();
    lexer.addErrorListener(new BaseErrorListener() {
      @Override
      public void syntaxError(Recognizer<?, ?> recognizer, Object o, int line, int pos, String msg, RecognitionException e) {
        errorReporter.report(new ParserError(new Concrete.Position(SOURCE_ID, line, pos), msg));
      }
    });

    CommonTokenStream tokens = new CommonTokenStream(lexer);
    VcgrammarParser parser = new VcgrammarParser(tokens);
    parser.removeErrorListeners();
    parser.addErrorListener(new BaseErrorListener() {
      @Override
      public void syntaxError(Recognizer<?, ?> recognizer, Object o, int line, int pos, String msg, RecognitionException e) {
        errorReporter.report(new ParserError(new Concrete.Position(SOURCE_ID, line, pos), msg));
      }
    });
    return parser;
  }


  Concrete.Expression parseExpr(String text, int errors) {
    VcgrammarParser.ExprContext ctx = _parse(text).expr();
    Concrete.Expression expr = errorList.isEmpty() ? new BuildVisitor(SOURCE_ID, errorReporter).visitExpr(ctx) : null;
    assertThat(errorList, containsErrors(errors));
    return expr;
  }

  protected Concrete.Expression parseExpr(String text) {
    return parseExpr(text, 0);
  }

  Concrete.Definition parseDef(String text, int errors) {
    VcgrammarParser.DefinitionContext ctx = _parse(text).definition();
    Concrete.Definition definition = errorList.isEmpty() ? new BuildVisitor(SOURCE_ID, errorReporter).visitDefinition(ctx) : null;
    assertThat(errorList, containsErrors(errors));
    return definition;
  }

  protected Concrete.Definition parseDef(String text) {
    return parseDef(text, 0);
  }

  Concrete.ClassDefinition parseClass(String name, String text, int errors) {
    VcgrammarParser.StatementsContext tree = _parse(text).statements();
    Concrete.ClassDefinition classDefinition = errorList.isEmpty() ? new Concrete.ClassDefinition(ConcreteExpressionFactory.POSITION, name, new BuildVisitor(SOURCE_ID, errorReporter).visitStatements(tree)) : null;
    assertThat(errorList, containsErrors(errors));
    // classDefinition.accept(new DefinitionResolveStaticModVisitor(new ConcreteStaticModListener()), null);
    return classDefinition;
  }

  protected Concrete.ClassDefinition parseClass(String name, String text) {
    return parseClass(name, text, 0);
  }


  protected static boolean compareAbstract(Abstract.Expression expr1, Abstract.Expression expr2) {
    return expr1.accept(new AbstractCompareVisitor(), expr2);
  }
}

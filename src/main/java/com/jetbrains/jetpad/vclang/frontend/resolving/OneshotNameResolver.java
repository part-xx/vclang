package com.jetbrains.jetpad.vclang.frontend.resolving;

import com.jetbrains.jetpad.vclang.frontend.resolving.visitor.DefinitionResolveNameVisitor;
import com.jetbrains.jetpad.vclang.naming.NameResolver;
import com.jetbrains.jetpad.vclang.naming.scope.Scope;
import com.jetbrains.jetpad.vclang.term.Abstract;

public class OneshotNameResolver {
  public static void visitModule(Abstract.ClassDefinition module, Scope globalScope, NameResolver nameResolver, NamespaceProviders nsProviders, ResolveListener resolveListener) {
    DefinitionResolveNameVisitor visitor = new DefinitionResolveNameVisitor(nsProviders, nameResolver, resolveListener);
    visitor.visitClass(module, globalScope);
  }
}

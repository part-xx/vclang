package com.jetbrains.jetpad.vclang.typechecking.order;

import com.jetbrains.jetpad.vclang.core.definition.Definition;
import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.typechecking.Typecheckable;

public class BaseDependencyListener implements DependencyListener {
  @Override
  public void sccFound(SCC scc) {

  }

  @Override
  public void alreadyTypechecked(Definition definition) {

  }

  @Override
  public void dependsOn(Typecheckable unit, Abstract.Definition def) {

  }
}

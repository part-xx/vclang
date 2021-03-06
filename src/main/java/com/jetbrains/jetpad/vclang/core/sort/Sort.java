package com.jetbrains.jetpad.vclang.core.sort;

import com.jetbrains.jetpad.vclang.core.subst.LevelSubstitution;
import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.typechecking.implicitargs.equations.DummyEquations;
import com.jetbrains.jetpad.vclang.typechecking.implicitargs.equations.Equations;

public class Sort {
  private final Level myPLevel;
  private final Level myHLevel;

  public static final Sort PROP = new Sort(null, new Level(0));
  public static final Sort SET0 = new Sort(new Level(0), new Level(1));
  public static final Sort SET = new Sort(Level.INFINITY, new Level(1));

  public static Sort SetOfLevel(int level) {
    return new Sort(level, 0);
  }

  public static Sort SetOfLevel(Level level) {
    return new Sort(level, new Level(1));
  }

  public static Sort TypeOfLevel(int level) {
    return new Sort(new Level(level), Level.INFINITY);
  }

  public Sort(int pLevel, int hLevel) {
    assert pLevel >= 0;
    assert hLevel >= 0;
    myPLevel = new Level(pLevel);
    myHLevel = new Level(hLevel + 1);
  }

  public Sort(Level plevel, Level hlevel) {
    myPLevel = hlevel.isMinimum() ? new Level(0) : plevel;
    myHLevel = hlevel;
  }

  public Level getPLevel() {
    return myPLevel;
  }

  public Level getHLevel() {
    return myHLevel;
  }

  public boolean isOmega() {
    return myPLevel.isInfinity();
  }

  public Sort succ() {
    return isProp() ? SET0 : new Sort(getPLevel().add(1), getHLevel().add(1));
  }

  public boolean isProp() {
    return myHLevel.isMinimum();
  }

  public static boolean compare(Sort sort1, Sort sort2, Equations.CMP cmp, Equations equations, Abstract.SourceNode sourceNode) {
    if (sort1.isProp()) {
      if (cmp == Equations.CMP.LE || sort2.isProp()) {
        return true;
      }
      return !sort2.getHLevel().isClosed() && equations.add(sort2.getHLevel(), new Level(0), Equations.CMP.EQ, sourceNode);
    }
    if (sort2.isProp()) {
      if (cmp == Equations.CMP.GE) {
        return true;
      }
      return !sort1.getHLevel().isClosed() && equations.add(sort1.getHLevel(), new Level(0), Equations.CMP.EQ, sourceNode);
    }
    return Level.compare(sort1.getPLevel(), sort2.getPLevel(), cmp, equations, sourceNode) && Level.compare(sort1.getHLevel(), sort2.getHLevel(), cmp, equations, sourceNode);
  }

  public boolean isLessOrEquals(Sort other) {
    return compare(this, other, Equations.CMP.LE, DummyEquations.getInstance(), null);
  }

  public Sort subst(LevelSubstitution subst) {
    return myPLevel.isClosed() && myHLevel.isClosed() ? this : new Sort(myPLevel.subst(subst), myHLevel.subst(subst));
  }

  @Override
  public boolean equals(Object other) {
    return other instanceof Sort && compare(this, (Sort) other, Equations.CMP.EQ, DummyEquations.getInstance(), null);
  }

  @Override
  public String toString() {
    if (isProp()) {
      return "\\Prop";
    }
    StringBuilder builder = new StringBuilder();
    boolean hlevelIsConstant = !myHLevel.isInfinity() && myHLevel.isClosed();
    if (hlevelIsConstant) {
      if (myHLevel.getConstant() == 1) {
        builder.append("\\Set");
      }
      builder.append("\\").append(myHLevel).append("-Type");
    } else {
      builder.append("\\Type");
    }

    if (hlevelIsConstant) {
      if (!myPLevel.isClosed()) {
        builder.append(" ");
      }
      builder.append(myPLevel);
    } else {
      builder.append("(").append(myPLevel).append(", ").append(myHLevel).append(")");
    }

    return builder.toString();
  }
}

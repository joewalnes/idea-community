package com.intellij.codeInsight.template.impl;

import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.template.Expression;
import com.intellij.codeInsight.template.ExpressionContext;
import com.intellij.codeInsight.template.Macro;
import com.intellij.codeInsight.template.Result;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;

/**
 *
 */
public class MacroCallNode extends Expression {
  public Macro getMacro() {
    return myMacro;
  }

  private final Macro myMacro;
  private final ArrayList<Expression> myParameters = new ArrayList<Expression>();

  public MacroCallNode(@NotNull Macro macro) {
    myMacro = macro;
  }

  public void addParameter(Expression node) {
    myParameters.add(node);
  }

  public Result calculateResult(ExpressionContext context) {
    Expression[] parameters = myParameters.toArray(new Expression[myParameters.size()]);
    return myMacro.calculateResult(parameters, context);
  }

  public Result calculateQuickResult(ExpressionContext context) {
    Expression[] parameters = myParameters.toArray(new Expression[myParameters.size()]);
    return myMacro.calculateQuickResult(parameters, context);
  }

  public LookupElement[] calculateLookupItems(ExpressionContext context) {
    Expression[] parameters = myParameters.toArray(new Expression[myParameters.size()]);
    return myMacro.calculateLookupItems(parameters, context);
  }

  public Expression[] getParameters() {
    return myParameters.toArray(new Expression[myParameters.size()]);
  }
}
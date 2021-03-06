/*
 * Copyright 2003-2008 Dave Griffith, Bas Leijdekkers
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.siyeh.ipp.conditional;

import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.codeStyle.CodeStyleManager;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.IncorrectOperationException;
import com.siyeh.ipp.base.Intention;
import com.siyeh.ipp.base.PsiElementPredicate;
import com.siyeh.ipp.psiutils.ParenthesesUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ReplaceConditionalWithIfIntention extends Intention {

    @Override
    @NotNull
    public PsiElementPredicate getElementPredicate() {
        return new ReplaceConditionalWithIfPredicate();
    }

    @Override
    public void processIntention(@NotNull PsiElement element)
            throws IncorrectOperationException {
        final PsiConditionalExpression expression =
                (PsiConditionalExpression)element;
        replaceConditionalWithIf(expression);
    }

    private static void replaceConditionalWithIf(
            PsiConditionalExpression expression)
            throws IncorrectOperationException {
        final PsiStatement statement =
                PsiTreeUtil.getParentOfType(expression, PsiStatement.class);
        if (statement == null) {
            return;
        }
        final PsiVariable variable;
        if (statement instanceof PsiDeclarationStatement) {
            variable =
                    PsiTreeUtil.getParentOfType(expression, PsiVariable.class);
        } else {
            variable = null;
        }
        final PsiExpression thenExpression = expression.getThenExpression();
        final PsiExpression elseExpression = expression.getElseExpression();
        final PsiExpression condition = expression.getCondition();
        final PsiExpression strippedCondition =
                ParenthesesUtils.stripParentheses(condition);
        final StringBuilder newStatement = new StringBuilder();
        newStatement.append("if(");
        if (strippedCondition != null) {
            newStatement.append(strippedCondition.getText());
        }
        newStatement.append(')');
        if (variable != null) {
            final String name = variable.getName();
            newStatement.append(name);
            newStatement.append('=');
            final PsiExpression initializer = variable.getInitializer();
            if (initializer == null) {
                return;
            }
            appendElementTextWithoutParentheses(initializer, expression,
                    thenExpression, newStatement);
            newStatement.append("; else ");
            newStatement.append(name);
            newStatement.append('=');
            appendElementTextWithoutParentheses(initializer, expression,
                    elseExpression, newStatement);
            newStatement.append(';');
            initializer.delete();
            final PsiManager manager = statement.getManager();
            final Project project = manager.getProject();
            final JavaPsiFacade facade = JavaPsiFacade.getInstance(project);
            final PsiElementFactory factory = facade.getElementFactory();
            final PsiStatement ifStatement = factory.createStatementFromText(
                    newStatement.toString(), statement);
            final PsiElement parent = statement.getParent();
            final PsiElement addedElement = parent.addAfter(ifStatement,
                    statement);
            final CodeStyleManager styleManager = manager.getCodeStyleManager();
            styleManager.reformat(addedElement);
        } else {
            final PsiElement expressionParent = expression.getParent();
            final boolean addBraces =
                    expressionParent instanceof PsiIfStatement;
            if (addBraces || thenExpression == null) {
                newStatement.append('{');
            }
            appendElementTextWithoutParentheses(statement, expression,
                    thenExpression, newStatement);
            if (addBraces) {
                newStatement.append("} else {");
            } else {
                if (thenExpression == null) {
                    newStatement.append('}');
                }
                newStatement.append(" else ");
                if (elseExpression == null) {
                    newStatement.append('{');
                }
            }
            appendElementTextWithoutParentheses(statement, expression,
                    elseExpression, newStatement);
            if (addBraces || elseExpression == null) {
                newStatement.append('}');
            }
            replaceStatement(newStatement.toString(), statement);
        }
    }

    private static void appendElementTextWithoutParentheses(
            @NotNull PsiElement element,
            @NotNull PsiElement elementToReplace,
            @Nullable PsiExpression replacementExpression,
            @NotNull StringBuilder out) {
        final PsiElement expressionParent = elementToReplace.getParent();
        if (expressionParent instanceof PsiParenthesizedExpression) {
            final PsiElement grandParent = expressionParent.getParent();
            if (!ParenthesesUtils.areParenthesesNeeded(replacementExpression,
                    grandParent, true)) {
                appendElementText(element, expressionParent,
                        replacementExpression, out);
                return;
            }
        }
        appendElementText(element, elementToReplace, replacementExpression,
                out);
    }

    private static void appendElementText(
            @NotNull PsiElement element,
            @NotNull PsiElement elementToReplace,
            @Nullable PsiExpression replacementExpression,
            @NotNull StringBuilder out) {
        if (element.equals(elementToReplace)) {
            final String replacementText = (replacementExpression == null) ?
                    "" : replacementExpression.getText();
            out.append(replacementText);
            return;
        }
        final PsiElement[] children = element.getChildren();
        if (children.length == 0) {
            out.append(element.getText());
            if (element instanceof PsiComment) {
                final PsiComment comment = (PsiComment)element;
                final IElementType tokenType = comment.getTokenType();
                if (tokenType == JavaTokenType.END_OF_LINE_COMMENT) {
                    out.append('\n');
                }
            }
            return;
        }
        for (PsiElement child : children) {
            appendElementText(child, elementToReplace, replacementExpression,
                    out);
        }
    }
}
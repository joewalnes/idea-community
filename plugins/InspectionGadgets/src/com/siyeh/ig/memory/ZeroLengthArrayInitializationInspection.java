/*
 * Copyright 2003-2007 Dave Griffith, Bas Leijdekkers
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
package com.siyeh.ig.memory;

import com.intellij.psi.*;
import com.intellij.psi.util.PsiTreeUtil;
import com.siyeh.InspectionGadgetsBundle;
import com.siyeh.ig.BaseInspection;
import com.siyeh.ig.BaseInspectionVisitor;
import com.siyeh.ig.InspectionGadgetsFix;
import com.siyeh.ig.fixes.IntroduceConstantFix;
import com.siyeh.ig.psiutils.ExpressionUtils;
import org.jetbrains.annotations.NotNull;

public class ZeroLengthArrayInitializationInspection extends BaseInspection {

    @NotNull
    public String getID() {
        return "ZeroLengthArrayAllocation";
    }

    @NotNull
    public String getDisplayName() {
        return InspectionGadgetsBundle.message(
                "array.allocation.zero.length.display.name");
    }

    @NotNull
    public String buildErrorString(Object... infos) {
        return InspectionGadgetsBundle.message(
                "array.allocation.zero.length.problem.descriptor");
    }

    public BaseInspectionVisitor buildVisitor() {
        return new ZeroLengthArrayInitializationVisitor();
    }

    protected InspectionGadgetsFix buildFix(Object... infos) {
        return new IntroduceConstantFix();
    }

    protected boolean buildQuickFixesOnlyForOnTheFlyErrors() {
        return true;
    }

    private static class ZeroLengthArrayInitializationVisitor
            extends BaseInspectionVisitor {

        @Override public void visitNewExpression(@NotNull PsiNewExpression expression) {
            super.visitNewExpression(expression);
            if (!ExpressionUtils.isZeroLengthArrayConstruction(expression)) {
                return;
            }
            if (isDeclaredConstant(expression)) {
                return;
            }
            registerError(expression);
        }

        @Override public void visitArrayInitializerExpression(
                PsiArrayInitializerExpression expression) {
            super.visitArrayInitializerExpression(expression);
            final PsiExpression[] initializers = expression.getInitializers();
            if (initializers.length > 0) {
                return;
            }
            if (expression.getParent() instanceof PsiNewExpression) {
                return;
            }
            if (isDeclaredConstant(expression)) {
                return;
            }
            registerError(expression);
        }

        private static boolean isDeclaredConstant(PsiExpression expression) {
            final PsiField field =
                    PsiTreeUtil.getParentOfType(expression, PsiField.class);
            if (field == null) {
                return false;
            }
            return field.hasModifierProperty(PsiModifier.STATIC) &&
                    field.hasModifierProperty(PsiModifier.FINAL);
        }
    }
}
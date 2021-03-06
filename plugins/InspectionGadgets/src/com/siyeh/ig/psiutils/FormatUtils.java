/*
 * Copyright 2010 Bas Leijdekkers
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
package com.siyeh.ig.psiutils;

import com.intellij.psi.*;
import org.jetbrains.annotations.NonNls;

import java.util.HashSet;
import java.util.Set;

public class FormatUtils {

    /**
     * @noinspection StaticCollection
     */
    @NonNls
    public static final Set<String> formatMethodNames =
            new HashSet<String>(2);
    /**
     * @noinspection StaticCollection
     */
    public static final Set<String> formatClassNames =
            new HashSet<String>(4);

    static {
        FormatUtils.formatMethodNames.add("format");
        FormatUtils.formatMethodNames.add("printf");

        FormatUtils.formatClassNames.add("java.io.PrintWriter");
        FormatUtils.formatClassNames.add("java.io.PrintStream");
        FormatUtils.formatClassNames.add("java.util.Formatter");
        FormatUtils.formatClassNames.add(CommonClassNames.JAVA_LANG_STRING);
    }

    private FormatUtils() {}

    public static boolean isFormatCall(
            PsiMethodCallExpression expression) {
        final PsiReferenceExpression methodExpression =
                expression.getMethodExpression();
        final String name = methodExpression.getReferenceName();
        if (!formatMethodNames.contains(name)) {
            return false;
        }
        final PsiMethod method = expression.resolveMethod();
        if (method == null) {
            return false;
        }
        final PsiClass containingClass = method.getContainingClass();
        if (containingClass == null) {
            return false;
        }
        final String className = containingClass.getQualifiedName();
        return formatClassNames.contains(className);
    }
}

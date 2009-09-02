/*
 * @author max
 */
package com.intellij.psi.impl.java.stubs;

import com.intellij.pom.java.LanguageLevel;
import com.intellij.psi.PsiClass;
import com.intellij.psi.stubs.NamedStub;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.Nullable;

public interface PsiClassStub<T extends PsiClass> extends NamedStub<T> {
  @NonNls
  @Nullable
  String getQualifiedName();

  @NonNls 
  @Nullable
  String getBaseClassReferenceText();

  boolean isDeprecated();
  boolean hasDeprecatedAnnotation();
  boolean isInterface();
  boolean isEnum();
  boolean isEnumConstantInitializer();
  boolean isAnonymous();
  boolean isAnonymousInQualifiedNew();
  boolean isAnnotationType();

  LanguageLevel getLanguageLevel();
  String getSourceFileName();
}
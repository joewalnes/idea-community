/*
 * Copyright 2000-2009 JetBrains s.r.o.
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
package com.intellij.packaging.impl.elements;

import com.intellij.openapi.compiler.CompilerBundle;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.packaging.elements.CompositePackagingElement;
import com.intellij.packaging.elements.CompositePackagingElementType;
import com.intellij.packaging.impl.ui.properties.ArchiveElementPropertiesPanel;
import com.intellij.packaging.ui.ArtifactEditorContext;
import com.intellij.packaging.ui.PackagingElementPropertiesPanel;
import com.intellij.util.Icons;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

/**
* @author nik
*/
class ArchiveElementType extends CompositePackagingElementType<ArchivePackagingElement> {
  ArchiveElementType() {
    super("archive", CompilerBundle.message("element.type.name.archive"));
  }

  @Override
  public Icon getCreateElementIcon() {
    return Icons.JAR_ICON;
  }

  @NotNull
  @Override
  public ArchivePackagingElement createEmpty(@NotNull Project project) {
    return new ArchivePackagingElement();
  }

  @Override
  public PackagingElementPropertiesPanel createElementPropertiesPanel(@NotNull ArchivePackagingElement element,
                                                                                               @NotNull ArtifactEditorContext context) {
    final String name = element.getArchiveFileName();
    if (name.length() >= 4 && name.charAt(name.length() - 4) == '.' && StringUtil.endsWithIgnoreCase(name, "ar")) {
      return new ArchiveElementPropertiesPanel(element, context);
    }
    return null;
  }

  public CompositePackagingElement<?> createComposite(CompositePackagingElement<?> parent, @Nullable String baseName, @NotNull ArtifactEditorContext context) {
    final String initialValue = PackagingElementFactoryImpl.suggestFileName(parent, baseName != null ? baseName : "archive", ".jar");
    final String path = Messages.showInputDialog(context.getProject(), "Enter archive name: ", "New Archive", null, initialValue, new FilePathValidator());
    if (path == null) return null;
    return PackagingElementFactoryImpl.createDirectoryOrArchiveWithParents(path, true);
  }
}

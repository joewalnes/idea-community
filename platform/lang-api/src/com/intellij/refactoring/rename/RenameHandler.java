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

package com.intellij.refactoring.rename;

import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.refactoring.RefactoringActionHandler;

/**
 * @author dsl
 */
public interface RenameHandler extends RefactoringActionHandler {
  ExtensionPointName<RenameHandler> EP_NAME = new ExtensionPointName<RenameHandler>("com.intellij.renameHandler");
  
  // called during rename action update. should not perform any user interactions
  boolean isAvailableOnDataContext(DataContext dataContext);
  // called on rename actionPeformed. Can obtain additional info from user
  boolean isRenaming(DataContext dataContext);
}

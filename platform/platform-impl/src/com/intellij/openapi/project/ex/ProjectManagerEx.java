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
package com.intellij.openapi.project.ex;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.util.InvalidDataException;
import com.intellij.openapi.vfs.VirtualFile;
import org.jdom.JDOMException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.TestOnly;

import java.io.IOException;

/**
 *
 */
public abstract class ProjectManagerEx extends ProjectManager {
  public static ProjectManagerEx getInstanceEx() {
    return (ProjectManagerEx)ApplicationManager.getApplication().getComponent(ProjectManager.class);
  }

  @Nullable
  public abstract Project newProject(final String projectName, String filePath, boolean useDefaultProjectSettings, boolean isDummy);

  @Nullable
  public abstract Project loadProject(String filePath) throws IOException, JDOMException, InvalidDataException;

  public abstract boolean openProject(Project project);

  public abstract boolean isProjectOpened(Project project);

  public abstract boolean canClose(Project project);

  public abstract void saveChangedProjectFile(VirtualFile file, final Project project);

  public abstract boolean isFileSavedToBeReloaded(VirtualFile file);

  public abstract void blockReloadingProjectOnExternalChanges();
  public abstract void unblockReloadingProjectOnExternalChanges();

  @Nullable
  @TestOnly
  public abstract Project getCurrentTestProject();

  @TestOnly
  public abstract void setCurrentTestProject(@Nullable Project project);

  @Nullable
  public abstract Project loadAndOpenProject(String filePath, boolean convert) throws IOException, JDOMException, InvalidDataException;

  // returns true on success
  public abstract boolean closeAndDispose(@NotNull Project project);
}

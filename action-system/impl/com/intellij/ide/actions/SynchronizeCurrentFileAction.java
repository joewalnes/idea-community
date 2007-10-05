/*
 * Copyright (c) 2000-2004 by JetBrains s.r.o. All Rights Reserved.
 * Use is subject to license terms.
 */
package com.intellij.ide.actions;

import com.intellij.ide.IdeBundle;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DataKeys;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.changes.VcsDirtyScopeManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.WindowManager;

public class SynchronizeCurrentFileAction extends AnAction {
  public void update(AnActionEvent e) {
    VirtualFile[] files = getFiles(e);

    if (getProject(e) == null || files == null || files.length == 0) {
      e.getPresentation().setEnabled(false);
      return;
    }

    String message = getMessage(files);
    e.getPresentation().setEnabled(true);
    e.getPresentation().setText(message);
  }

  private static String getMessage(VirtualFile[] files) {
    if (files.length == 1) {
      return IdeBundle.message("action.synchronize.file", files[0].getName().replace("_", "__").replace("&", "&&"));
    }
    return IdeBundle.message("action.synchronize.selected.files");
  }

  public void actionPerformed(AnActionEvent e) {
    final Project project = getProject(e);
    final VirtualFile[] files = getFiles(e);

    ApplicationManager.getApplication().runWriteAction(new Runnable() {
      public void run() {
        for (VirtualFile f : files) {
          f.refresh(false, true);
        }
      }
    });

    VcsDirtyScopeManager dirtyScopeManager = VcsDirtyScopeManager.getInstance(project);
    for (VirtualFile f : files) {
      if (f.isDirectory()) {
        dirtyScopeManager.dirDirtyRecursively(f);
      }
      else {
        dirtyScopeManager.fileDirty(f);
      }
    }

    String message = IdeBundle.message("action.sync.completed.successfully", getMessage(files));
    WindowManager.getInstance().getStatusBar(project).setInfo(message);
  }

  private static Project getProject(AnActionEvent e) {
    return e.getData(DataKeys.PROJECT);
  }

  private static VirtualFile[] getFiles(AnActionEvent e) {
    return e.getData(DataKeys.VIRTUAL_FILE_ARRAY);
  }
}
package com.intellij.openapi.actionSystem.ex;

import com.intellij.openapi.actionSystem.ActionGroup;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.IndexNotReadyException;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;
import org.jetbrains.annotations.NonNls;

import java.util.ArrayList;
import java.util.List;

public class ActionUtil {
  @NonNls private static final String WAS_ENABLED_BEFORE_DUMB = "WAS_ENABLED_BEFORE_DUMB";
  @NonNls public static final String WOULD_BE_ENABLED_IF_NOT_DUMB_MODE = "WOULD_BE_ENABLED_IF_NOT_DUMB_MODE";
  @NonNls private static final String WOULD_BE_VISIBLE_IF_NOT_DUMB_MODE = "WOULD_BE_VISIBLE_IF_NOT_DUMB_MODE";

  private ActionUtil() {
  }

  public static void showDumbModeWarning(AnActionEvent... events) {
    Project project = null;
    List<String> actionNames = new ArrayList<String>();
    for (final AnActionEvent event : events) {
      final String s = event.getPresentation().getText();
      if (StringUtil.isNotEmpty(s)) {
        actionNames.add(s);
      }

      final Project _project = (Project)event.getDataContext().getData(DataConstantsEx.PROJECT);
      if (_project != null && project == null) {
        project = _project;
      }
    }

    if (project == null) {
      return;
    }

    String message;
    final String beAvailableUntil = " available while IntelliJ IDEA is updating indices";
    if (actionNames.isEmpty()) {
      message = "This action is not" + beAvailableUntil;
    } else if (actionNames.size() == 1) {
      message = "'" + actionNames.get(0) + "' action is not" + beAvailableUntil;
    } else {
      message = "None of the following actions are" + beAvailableUntil + ": " + StringUtil.join(actionNames, ", ");
    }

    DumbService.getInstance(project).showDumbModeNotification(message);
  }

  /**
   * @param action action
   * @param e action event
   * @param beforeActionPerformed whether to call
   * {@link com.intellij.openapi.actionSystem.AnAction#beforeActionPerformedUpdate(com.intellij.openapi.actionSystem.AnActionEvent)}
   * or
   * {@link com.intellij.openapi.actionSystem.AnAction#update(com.intellij.openapi.actionSystem.AnActionEvent)}
   * @return true if update tried to access indices in dumb mode
   */
  public static boolean performDumbAwareUpdate(AnAction action, AnActionEvent e, boolean beforeActionPerformed) {
    final Presentation presentation = e.getPresentation();
    final Boolean wasEnabledBefore = (Boolean)presentation.getClientProperty(WAS_ENABLED_BEFORE_DUMB);
    final Project project = (Project)e.getDataContext().getData(DataConstantsEx.PROJECT);
    final boolean dumbMode = project != null && DumbService.getInstance(project).isDumb();
    if (wasEnabledBefore != null && !dumbMode) {
      presentation.putClientProperty(WAS_ENABLED_BEFORE_DUMB, null);
      presentation.setEnabled(wasEnabledBefore.booleanValue());
      presentation.setVisible(true);
    }
    final boolean enabledBeforeUpdate = presentation.isEnabled();

    final boolean notAllowed = dumbMode && !(action instanceof DumbAware) && !(action instanceof ActionGroup);

    try {
      if (beforeActionPerformed) {
        action.beforeActionPerformedUpdate(e);
      }
      else {
        action.update(e);
      }
      presentation.putClientProperty(WOULD_BE_ENABLED_IF_NOT_DUMB_MODE, notAllowed && presentation.isEnabled());
      presentation.putClientProperty(WOULD_BE_VISIBLE_IF_NOT_DUMB_MODE, notAllowed && presentation.isVisible());
    }
    catch (IndexNotReadyException e1) {
      if (notAllowed) {
        return true;
      }
      throw e1;
    }
    finally {
      if (notAllowed) {
        if (wasEnabledBefore == null) {
          presentation.putClientProperty(WAS_ENABLED_BEFORE_DUMB, enabledBeforeUpdate);
        }
        presentation.setEnabled(false);
        presentation.setVisible(false);
      }
    }
    
    return false;
  }

  public static boolean lastUpdateAndCheckDumb(AnAction action, AnActionEvent e, boolean visibilityMatters) {
    performDumbAwareUpdate(action, e, true);

    final Project project = (Project)e.getDataContext().getData(DataConstantsEx.PROJECT);
    if (project != null && DumbService.getInstance(project).isDumb() && !(action instanceof DumbAware)) {
      if (Boolean.FALSE.equals(e.getPresentation().getClientProperty(WOULD_BE_ENABLED_IF_NOT_DUMB_MODE))) {
        return false;
      }
      if (visibilityMatters && Boolean.FALSE.equals(e.getPresentation().getClientProperty(WOULD_BE_VISIBLE_IF_NOT_DUMB_MODE))) {
        return false;
      }

      showDumbModeWarning(e);
      return false;
    }

    if (!e.getPresentation().isEnabled()) {
      return false;
    }
    if (visibilityMatters && !e.getPresentation().isVisible()) {
      return false;
    }

    return true;
  }

}
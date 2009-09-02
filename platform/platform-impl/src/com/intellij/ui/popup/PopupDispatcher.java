/*
 * Copyright (c) 2000-2004 by JetBrains s.r.o. All Rights Reserved.
 * Use is subject to license terms.
 */
package com.intellij.ui.popup;

import com.intellij.ide.IdeEventQueue;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ex.ApplicationManagerEx;
import com.intellij.openapi.ui.popup.IdePopupEventDispatcher;

import javax.swing.*;
import java.awt.*;
import java.awt.event.AWTEventListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;

public class PopupDispatcher implements AWTEventListener, KeyEventDispatcher, IdePopupEventDispatcher {

  private static WizardPopup ourActiveWizardRoot;
  private static WizardPopup ourShowingStep;

  private static final PopupDispatcher ourInstance = new PopupDispatcher();

  static {
    if (System.getProperty("is.popup.test") != null ||
      (ApplicationManagerEx.getApplicationEx() != null && ApplicationManagerEx.getApplicationEx().isUnitTestMode())) {
      Toolkit.getDefaultToolkit().addAWTEventListener(ourInstance, MouseEvent.MOUSE_PRESSED);
      KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(ourInstance);
    }
  }

  private PopupDispatcher() {
  }

  public static PopupDispatcher getInstance() {
    return ourInstance;
  }

  public static void setActiveRoot(WizardPopup aRootPopup) {
    disposeActiveWizard();
    ourActiveWizardRoot = aRootPopup;
    ourShowingStep = aRootPopup;
    if (ApplicationManager.getApplication() != null) {
      IdeEventQueue.getInstance().getPopupManager().push(ourInstance);
    }
  }

  public static void clearRootIfNeeded(WizardPopup aRootPopup) {
    if (ourActiveWizardRoot == aRootPopup) {
      ourActiveWizardRoot = null;
      ourShowingStep = null;
      if (ApplicationManager.getApplication() != null) {
        IdeEventQueue.getInstance().getPopupManager().remove(ourInstance);
      }
    }
  }

  public void eventDispatched(AWTEvent event) {
    dispatchMouseEvent(event);
  }

  private boolean dispatchMouseEvent(AWTEvent event) {
    if (event.getID() != MouseEvent.MOUSE_PRESSED) {
      return false;
    }

    if (ourShowingStep == null) {
      return false;
    }

    WizardPopup eachParent = ourShowingStep;
    final MouseEvent mouseEvent = ((MouseEvent) event);

    Point point = (Point) mouseEvent.getPoint().clone();
    SwingUtilities.convertPointToScreen(point, mouseEvent.getComponent());

    while (true) {
      if (!eachParent.getContent().isShowing()) {
        getActiveRoot().cancel();
        return false;
      }

      if (eachParent.getBounds().contains(point) || !eachParent.canClose()) {
        return false;
      }

      eachParent = eachParent.getParent();
      if (eachParent == null) {
        getActiveRoot().cancel();
        return false;
      }
    }
  }

  public static boolean disposeActiveWizard() {
    if (ourActiveWizardRoot != null) {
      ourActiveWizardRoot.disposeChildren();
      ourActiveWizardRoot.dispose();
      return true;
    }

    return false;
  }

  public boolean dispatchKeyEvent(final KeyEvent e) {
    if (ourShowingStep == null) {
      return false;
    }
    ourShowingStep.dispatch(e);

    return true;
  }

  public static void setShowing(WizardPopup aBaseWizardPopup) {
    ourShowingStep = aBaseWizardPopup;
  }

  public static void unsetShowing(WizardPopup aBaseWizardPopup) {
    ourShowingStep = aBaseWizardPopup.getParent();
  }

  public static WizardPopup getActiveRoot() {
    return ourActiveWizardRoot;
  }

  public static boolean isWizardShowing() {
    return ourActiveWizardRoot != null;
  }

  public Component getComponent() {
    return ourShowingStep != null ? ourShowingStep.getContent() : null;
  }

  public boolean dispatch(AWTEvent event) {
   if (event instanceof KeyEvent) {
      return dispatchKeyEvent(((KeyEvent) event));
   } else if (event instanceof MouseEvent) {
     return dispatchMouseEvent(event);
   } else {
     return false;
   }
  }

  public void requestFocus() {
    if (ourShowingStep != null) {
      ourShowingStep.requestFocus();
    }
  }

  public boolean close() {
    final String s = "sdfsf";

    return disposeActiveWizard();
  }
}
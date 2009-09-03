/*
 * Copyright (c) 2000-2006 JetBrains s.r.o. All Rights Reserved.
 */

package com.intellij.uiDesigner.propertyInspector.editors;

import com.intellij.uiDesigner.propertyInspector.PropertyEditor;
import com.intellij.uiDesigner.propertyInspector.InplaceContext;
import com.intellij.uiDesigner.radComponents.RadComponent;
import com.intellij.util.ui.UIUtil;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * @author yole
 */
public abstract class AbstractTextFieldEditor<V> extends PropertyEditor<V> {
  protected final JTextField myTf;

  protected AbstractTextFieldEditor() {
    myTf = new JTextField();
    myTf.addActionListener(new MyActionListener());
  }

  public void updateUI() {
    SwingUtilities.updateComponentTreeUI(myTf);
  }

  protected void setValueFromComponent(RadComponent component, V value) {
    myTf.setText(value == null ? "" : value.toString());
  }

  public JComponent getComponent(final RadComponent ignored, final V value, final InplaceContext inplaceContext) {
    setValueFromComponent(ignored, value);

    if(inplaceContext != null) {
      myTf.setBorder(UIUtil.getTextFieldBorder());
      if (inplaceContext.isStartedByTyping()) {
        myTf.setText(Character.toString(inplaceContext.getStartChar()));
      }
    }
    else{
      myTf.setBorder(null);
    }

    return myTf;
  }

  protected final class MyActionListener implements ActionListener {
    public void actionPerformed(final ActionEvent e){
      fireValueCommitted(true, false);
    }
  }
}